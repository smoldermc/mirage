package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.port.MirageScheduler;
import fr.smolder.mirage.core.port.PlatformAdapter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class MinestomPlatformAdapter implements PlatformAdapter {
    private final Path dataDirectory;
    private final MirageScheduler scheduler = new MirageScheduler() {
        @Override
        public void executeAsync(Runnable task) {
            CompletableFuture.runAsync(task);
        }

        @Override
        public void schedule(Duration delay, Runnable task) {
            MinecraftServer.getSchedulerManager()
                    .buildTask(task)
                    .delay(TaskSchedule.duration(delay))
                    .schedule();
        }
    };

    public MinestomPlatformAdapter(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Logger logger() {
        return MinecraftServer.LOGGER;
    }

    @Override
    public MirageScheduler scheduler() {
        return scheduler;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }
}
