package io.iridium.overvaults.millenium.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.iridium.overvaults.OverVaults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = OverVaults.MOD_ID)
public class CommandRegistry {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommand(StructureCommands::new, dispatcher);
        registerCommand(LootTableCommands::new, dispatcher);
        registerCommand(DebugCommands::new, dispatcher);
        registerCommand(PerformanceCommands::new, dispatcher);
    }

    public static <T extends BaseCommand> T registerCommand(Supplier<T> supplier, CommandDispatcher<CommandSourceStack> dispatcher) {
        T command = supplier.get();
        command.registerCommand(dispatcher);
        return command;
    }


}
