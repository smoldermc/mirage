package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.MirageRuntime;
import fr.smolder.mirage.core.port.PermissionProvider;
import net.minestom.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Objects;

public final class MinestomMirageBootstrap {
    private MirageRuntime runtime;
    private MinestomPlatformAdapter platformAdapter;

    public void install(Path dataDirectory) {
        install(dataDirectory, PermissionProvider.allowAll());
    }

    public void install(Path dataDirectory, PermissionProvider permissionProvider) {
        Objects.requireNonNull(permissionProvider, "permissionProvider");
        this.platformAdapter = new MinestomPlatformAdapter(dataDirectory);
        this.runtime = new MirageRuntime(platformAdapter);

        MinestomMotdController motdController = new MinestomMotdController();
        motdController.install((key, protocolVersion) -> runtime.motd(key, protocolVersion));
        new MinestomCommandRegistrar(permissionProvider).registerReloadCommand(
                () -> {
                    System.out.println("[Mirage] Received reload request.");
                    MinecraftServer.LOGGER.info("Received Mirage reload request.");
                    platformAdapter.scheduler().executeAsync(runtime::reload);
                }
        );

        System.out.println("[Mirage] Scheduling initial reload.");
        MinecraftServer.LOGGER.info("Scheduling initial Mirage reload.");
        platformAdapter.scheduler().executeAsync(runtime::reload);
        MinecraftServer.LOGGER.info("Mirage runtime installed for Minestom at {}", dataDirectory);
    }

    public void close() {
        if (runtime != null) {
            runtime.close();
        }
        if (platformAdapter != null) {
            platformAdapter.close();
        }
    }

    MirageRuntime runtime() {
        return runtime;
    }
}
