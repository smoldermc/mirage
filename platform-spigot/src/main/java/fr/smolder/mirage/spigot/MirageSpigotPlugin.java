package fr.smolder.mirage.spigot;

import fr.smolder.mirage.core.MirageRuntime;
import org.bukkit.plugin.java.JavaPlugin;

public final class MirageSpigotPlugin extends JavaPlugin {
    private SpigotPlatformAdapter platformAdapter;
    private MirageRuntime runtime;

    @Override
    public void onEnable() {
        this.platformAdapter = new SpigotPlatformAdapter(this);
        this.runtime = new MirageRuntime(platformAdapter);

        SpigotMotdListener motdListener = new SpigotMotdListener();
        motdListener.install((key, protocolVersion) -> runtime.motd(key, protocolVersion));
        getServer().getPluginManager().registerEvents(motdListener, this);

        new SpigotCommandRegistrar(this).registerReloadCommand(
                () -> platformAdapter.scheduler().executeAsync(runtime::reload)
        );
        platformAdapter.scheduler().executeAsync(runtime::reload);
        platformAdapter.logger().info("Mirage runtime enabled for Paper/Spigot.");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
        }
    }
}
