package io.iridium.overvaults.network;

import io.iridium.overvaults.OverVaults;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class OverVaultsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OverVaults.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                ClientboundOvervaultCompassPacket.class,
                ClientboundOvervaultCompassPacket::encode,
                ClientboundOvervaultCompassPacket::decode,
                ClientboundOvervaultCompassPacket::handle
        );
    }
}
