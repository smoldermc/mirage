package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.MirageRuntime;
import net.minestom.server.MinecraftServer;

import java.nio.file.Path;

public final class MinestomMirageBootstrap {
    private MirageRuntime runtime;
    private MinestomPlatformAdapter platformAdapter;

    public void install(Path dataDirectory) {
        this.platformAdapter = new MinestomPlatformAdapter(dataDirectory);
        this.runtime = new MirageRuntime(platformAdapter);

        MinestomMotdController motdController = new MinestomMotdController();
        motdController.install((key, protocolVersion) -> runtime.motd(key, protocolVersion));
        new MinestomCommandRegistrar().registerReloadCommand(
                () -> platformAdapter.scheduler().executeAsync(runtime::reload)
        );

        platformAdapter.scheduler().executeAsync(runtime::reload);
        MinecraftServer.LOGGER.info("Mirage runtime installed for Minestom at {}", dataDirectory);
    }

    public void close() {
        if (runtime != null) {
            runtime.close();
        }
    }
}
