package fr.smolder.mirage.minestom;

import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinestomManualTestServer {
    private MinestomManualTestServer() {
    }

    public static void main(String[] args) throws IOException {
        Path dataDirectory = args.length > 0
                ? Path.of(args[0]).toAbsolutePath()
                : Path.of("run", "minestom-manual").toAbsolutePath();
        Files.createDirectories(dataDirectory);

        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Offline());

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0.5, 42, 0.5));
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) {
                return;
            }

            event.getPlayer().setGameMode(GameMode.CREATIVE);
            event.getPlayer().sendMessage(Component.text("Mirage manual test server is running."));
            event.getPlayer().sendMessage(Component.text("Edit config.yml/logo.png in the manual data directory, then run /mirage."));
        });

        MinestomMirageBootstrap bootstrap = new MinestomMirageBootstrap();
        bootstrap.install(dataDirectory);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bootstrap.close();
            MinecraftServer.stopCleanly();
        }, "mirage-manual-shutdown"));

        System.out.println("Mirage manual Minestom server starting on 0.0.0.0:25565");
        System.out.println("Data directory: " + dataDirectory);
        System.out.println("Edit config.yml and assets there, then use /mirage to reload.");

        minecraftServer.start("0.0.0.0", 25565);
    }
}
