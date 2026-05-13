package io.iridium.overvaults.mixin;

import io.iridium.overvaults.client.OvervaultCompassHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "iskallia.vault.init.ModModels$ItemProperty$CompassPropertyFunction", remap = false)
public class MixinCompassPropertyFunction {

    @Inject(method = "unclampedCall", at = @At("HEAD"), cancellable = true)
    private void onUnclampedCall(ItemStack compass, ClientLevel level, LivingEntity entity, int seed, CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }

        ClientLevel clientLevel = level;
        if (clientLevel == null && entity.level instanceof ClientLevel entityLevel) {
            clientLevel = entityLevel;
        }

        if (!OvervaultCompassHandler.shouldApplyOvervaultTarget(clientLevel) || !OvervaultCompassHandler.hasTargetFor(clientLevel)) {
            return;
        }

        BlockPos target = OvervaultCompassHandler.getTarget();
        if (target == null || player.position().distanceToSqr(target.getX() + 0.5D, player.position().y(), target.getZ() + 0.5D) <= 1.0E-5D) {
            return;
        }

        cir.setReturnValue(getRotationTo(target, player));
    }

    @Inject(method = "getCompassTarget", at = @At("RETURN"), cancellable = true)
    private void onGetCompassTarget(Player player, ClientLevel level, ItemStack compass, int seed, CallbackInfoReturnable<BlockPos> cir) {
        if (OvervaultCompassHandler.shouldApplyOvervaultTarget(level)) {
            if (OvervaultCompassHandler.hasTargetFor(level)) {
                cir.setReturnValue(OvervaultCompassHandler.getTarget());
            }
        }
    }

    @Unique
    private float getRotationTo(BlockPos target, Player player) {
        Vec3 targetCenter = Vec3.atCenterOf(target);
        double angle = Math.atan2(targetCenter.z() - player.getZ(), targetCenter.x() - player.getX()) / (Math.PI * 2.0D);
        double playerRotation = Mth.positiveModulo(player.getYRot() / 360.0D, 1.0D);
        return Mth.positiveModulo((float)(angle + 0.75D - playerRotation), 1.0F);
    }
}
