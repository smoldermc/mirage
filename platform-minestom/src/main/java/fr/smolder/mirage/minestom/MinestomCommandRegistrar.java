package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.port.CommandRegistrar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

public final class MinestomCommandRegistrar implements CommandRegistrar {
    @Override
    public void registerReloadCommand(Runnable reloadAction) {
        Command command = new Command("mirage");
        command.setDefaultExecutor((sender, context) -> {
            reloadAction.run();
            sender.sendMessage(Component.text("Mirage reload queued."));
        });
        MinecraftServer.getCommandManager().register(command);
    }
}
