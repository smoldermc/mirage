package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.port.CommandRegistrar;
import fr.smolder.mirage.core.port.PermissionProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

import java.util.Objects;

public final class MinestomCommandRegistrar implements CommandRegistrar {
    private final PermissionProvider permissionProvider;

    public MinestomCommandRegistrar() {
        this(PermissionProvider.allowAll());
    }

    public MinestomCommandRegistrar(PermissionProvider permissionProvider) {
        this.permissionProvider = Objects.requireNonNull(permissionProvider, "permissionProvider");
    }

    @Override
    public void registerReloadCommand(Runnable reloadAction) {
        Command command = new Command("mirage");
        command.setDefaultExecutor((sender, context) -> {
            if (!permissionProvider.canExecute(sender)) {
                return;
            }
            reloadAction.run();
            sender.sendMessage(Component.text("Mirage reload queued."));
        });
        MinecraftServer.getCommandManager().register(command);
    }
}
