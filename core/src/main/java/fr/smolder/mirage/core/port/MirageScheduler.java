package fr.smolder.mirage.core.port;

import java.time.Duration;

public interface MirageScheduler {
    void executeAsync(Runnable task);

    void schedule(Duration delay, Runnable task);
}
