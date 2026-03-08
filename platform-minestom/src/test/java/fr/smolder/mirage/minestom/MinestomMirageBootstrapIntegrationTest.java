package fr.smolder.mirage.minestom;

import fr.smolder.mirage.core.cache.SqliteSkinCache;
import fr.smolder.mirage.core.image.TileHasher;
import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.SkinData;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.ping.ServerListPingType;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnvTest
class MinestomMirageBootstrapIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void installRegistersProtocolAwareMotdResponses(Env env) throws Exception {
        writeConfigAndImage(tempDir);
        seedPersistentCache(tempDir.resolve("logo.png"), tempDir.resolve("mirage-cache.sqlite"));

        MinestomMirageBootstrap bootstrap = new MinestomMirageBootstrap();
        bootstrap.install(tempDir);

        assertTrue(
                waitForCommand(env, "mirage", Duration.ofSeconds(5)),
                "Mirage command was not registered in time."
        );
        assertTrue(
                waitForFallback(env, "Legacy fallback", Duration.ofSeconds(5)),
                "Mirage runtime did not finish loading the configured fallback MOTD in time."
        );
        assertTrue(
                waitForModernReady(env, bootstrap, Duration.ofSeconds(5)),
                "Mirage runtime did not finish preparing the modern MOTD in time."
        );

        ServerListPingEvent legacyPing = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
        dispatchAsync(legacyPing);
        assertEquals(Component.text("Legacy fallback"), legacyPing.getStatus().description());

        MotdRender modernRender = bootstrap.runtime().motd("default", 774);
        assertTrue(modernRender.modernJson().contains("\"type\":\"object\""));
        assertTrue(modernRender.modernJson().contains("\"minecraft:player\""));

        bootstrap.close();
    }

    private static boolean waitForCommand(Env env, String command, Duration timeout) {
        return env.tickWhile(() -> env.process().command().getCommand(command) == null, timeout);
    }

    private static boolean waitForFallback(Env env, String expectedText, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ServerListPingEvent ping = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
            dispatchAsync(ping);
            if (Component.text(expectedText).equals(ping.getStatus().description())) {
                return true;
            }
            env.tick();
            Thread.sleep(10L);
        }
        return false;
    }

    private static boolean waitForModernReady(Env env, MinestomMirageBootstrap bootstrap, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            MotdRender render = bootstrap.runtime().motd("default", 774);
            if (render.state() == MotdRender.RenderState.READY && render.modernJson().contains("\"type\":\"object\"")) {
                return true;
            }
            env.tick();
            Thread.sleep(10L);
        }
        return false;
    }

    private static void dispatchAsync(ServerListPingEvent event) throws InterruptedException {
        Thread thread = Thread.ofVirtual().start(() -> EventDispatcher.call(event));
        thread.join();
    }

    private void writeConfigAndImage(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("config.yml"), """
                settings:
                  mineskin_api_key: ""
                  database_type: "sqlite"
                  minimum_modern_protocol: 774

                images:
                  server_logo:
                    file: "logo.png"
                    shadow_color: "#00000000"

                motd:
                  default:
                    type: "image"
                    target_image: "server_logo"
                    fallback_text: "Legacy fallback"
                """);

        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, 0xFFFF8800);
            }
        }
        ImageIO.write(image, "png", dataDirectory.resolve("logo.png").toFile());
    }

    private void seedPersistentCache(Path imagePath, Path databasePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        String tileHash = TileHasher.hashTile(image, 0, 0, 8);

        try (SqliteSkinCache cache = new SqliteSkinCache(databasePath)) {
            cache.put(tileHash, new SkinData("texture-value", "signature-value"));
        }
    }

    private static final class StubConnection extends PlayerConnection {
        private final int protocolVersion;

        private StubConnection(int protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        @Override
        public void sendPacket(SendablePacket packet) {
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 25565);
        }

        @Override
        public int getProtocolVersion() {
            return protocolVersion;
        }
    }
}
