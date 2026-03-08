package fr.smolder.mirage.spigot;

import fr.smolder.mirage.core.port.CommandRegistrar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotCommandRegistrar implements CommandRegistrar {
    private final JavaPlugin plugin;

    public SpigotCommandRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerReloadCommand(Runnable reloadAction) {
        PluginCommand command = plugin.getCommand("mirage");
        if (command == null) {
            plugin.getLogger().warning("Command 'mirage' is missing from plugin.yml.");
            return;
        }

        command.setExecutor(new ReloadCommandExecutor(reloadAction));
    }

    private record ReloadCommandExecutor(Runnable reloadAction) implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            reloadAction.run();
            sender.sendMessage("Mirage reload queued.");
            return true;
        }
    }
}
