package fr.smolder.mirage.spigot;

import fr.smolder.mirage.core.port.MirageScheduler;
import fr.smolder.mirage.core.port.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;

public final class SpigotPlatformAdapter implements PlatformAdapter {
    private final JavaPlugin plugin;
    private final Logger logger = LoggerFactory.getLogger(SpigotPlatformAdapter.class);
    private final MirageScheduler scheduler;

    public SpigotPlatformAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new MirageScheduler() {
            @Override
            public void executeAsync(Runnable task) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }

            @Override
            public void schedule(Duration delay, Runnable task) {
                long ticks = Math.max(1L, delay.toMillis() / 50L);
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks);
            }
        };
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public MirageScheduler scheduler() {
        return scheduler;
    }

    @Override
    public Path dataDirectory() {
        return plugin.getDataFolder().toPath();
    }
}
