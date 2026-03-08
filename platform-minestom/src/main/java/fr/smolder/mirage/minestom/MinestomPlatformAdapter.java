package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.port.MirageScheduler;
import fr.smolder.mirage.core.port.PlatformAdapter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class MinestomPlatformAdapter implements PlatformAdapter {
    private final Path dataDirectory;
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(daemonFactory("mirage-minestom-async"));
    private final MirageScheduler scheduler = new MirageScheduler() {
        @Override
        public void executeAsync(Runnable task) {
            asyncExecutor.execute(wrap(task, "async"));
        }

        @Override
        public void schedule(Duration delay, Runnable task) {
            MinecraftServer.getSchedulerManager()
                    .buildTask(wrap(task, "scheduled"))
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

    public void close() {
        asyncExecutor.shutdownNow();
    }

    private Runnable wrap(Runnable task, String source) {
        return () -> {
            try {
                System.out.println("[Mirage] Running " + source + " task.");
                logger().info("Running Mirage {} task.", source);
                task.run();
            } catch (Throwable throwable) {
                System.err.println("[Mirage] " + source + " task failed: " + throwable.getMessage());
                throwable.printStackTrace(System.err);
                logger().error("Mirage {} task failed.", source, throwable);
            }
        };
    }

    private static ThreadFactory daemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
