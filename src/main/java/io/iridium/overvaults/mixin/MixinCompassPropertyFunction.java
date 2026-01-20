package io.iridium.overvaults.mixin;

import io.iridium.overvaults.client.OvervaultCompassHandler;
import iskallia.vault.core.vault.ClientVaults;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "iskallia.vault.init.ModModels$ItemProperty$CompassPropertyFunction", remap = false)
public class MixinCompassPropertyFunction {

    @Inject(method = "getCompassTarget", at = @At("RETURN"), cancellable = true)
    private void onGetCompassTarget(Player player, ClientLevel level, ItemStack compass, int seed, CallbackInfoReturnable<BlockPos> cir) {
        if (cir.getReturnValue() == null && ClientVaults.getActive().isEmpty()) {
            BlockPos overvaultTarget = OvervaultCompassHandler.getTarget();
            if (overvaultTarget != null) {
                cir.setReturnValue(overvaultTarget);
            }
        }
    }
}
