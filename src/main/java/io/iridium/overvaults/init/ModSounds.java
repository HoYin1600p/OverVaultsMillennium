package io.iridium.overvaults.init;

import io.iridium.overvaults.OverVaults;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OverVaults.MOD_ID);

    public static final RegistryObject<SoundEvent> PORTAL_SPAWN = registerSoundEvent("portal_spawn");




    private static RegistryObject<SoundEvent> registerSoundEvent(String name){
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(OverVaults.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus){
        SOUNDS.register(eventBus);
    }
}
