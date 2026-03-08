package fr.smolder.mirage.core;

import fr.smolder.mirage.core.cache.InMemorySkinCache;
import fr.smolder.mirage.core.cache.SkinCache;
import fr.smolder.mirage.core.cache.SqliteSkinCache;
import fr.smolder.mirage.core.config.ConfigLoader;
import fr.smolder.mirage.core.config.MirageConfig;
import fr.smolder.mirage.core.image.SkinTileSlicer;
import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.SkinData;
import fr.smolder.mirage.core.model.TileSkin;
import fr.smolder.mirage.core.port.PlatformAdapter;
import fr.smolder.mirage.core.service.MotdRenderService;
import fr.smolder.mirage.core.text.ObjectComponentJsonGenerator;
import fr.smolder.mirage.core.upload.MineskinClient;
import fr.smolder.mirage.core.upload.NoopMineskinClient;
import fr.smolder.mirage.core.upload.RealMineskinClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MirageRuntime implements AutoCloseable {
    private static final String DEFAULT_MOTD_KEY = "default";

    private final PlatformAdapter platformAdapter;
    private final ConfigLoader configLoader = new ConfigLoader();
    private final ConcurrentMap<String, MotdRender> renders = new ConcurrentHashMap<>();

    private volatile MirageConfig config = MirageConfig.defaults();
    private volatile SkinCache skinCache = new InMemorySkinCache();
    private volatile MineskinClient mineskinClient = new NoopMineskinClient();

    public MirageRuntime(PlatformAdapter platformAdapter) {
        this.platformAdapter = platformAdapter;
        this.renders.put(DEFAULT_MOTD_KEY, MotdRender.loading("Loading...", List.of()));
    }

    public synchronized void reload() {
        try {
            Files.createDirectories(platformAdapter.dataDirectory());
            ensureDefaultConfig(platformAdapter.dataDirectory().resolve("config.yml"));

            MirageConfig loadedConfig = configLoader.load(platformAdapter.dataDirectory().resolve("config.yml"));
            closeCurrentResources();

            this.config = loadedConfig;
            this.skinCache = new SqliteSkinCache(platformAdapter.dataDirectory().resolve("mirage-cache.sqlite"));
            this.mineskinClient = loadedConfig.settings().mineskinApiKey().isBlank()
                    ? new NoopMineskinClient()
                    : new RealMineskinClient(loadedConfig.settings().mineskinApiKey());

            MotdRenderService renderService = new MotdRenderService(
                    new SkinTileSlicer(),
                    skinCache,
                    new ObjectComponentJsonGenerator()
            );

            Map<String, MotdRender> refreshed = new LinkedHashMap<>();
            for (var entry : loadedConfig.motds().entrySet()) {
                refreshed.put(entry.getKey(), renderMotd(renderService, loadedConfig, entry.getValue()));
            }
            if (refreshed.isEmpty()) {
                refreshed.put(DEFAULT_MOTD_KEY, MotdRender.loading("Loading...", List.of()));
            }

            renders.clear();
            renders.putAll(refreshed);
            platformAdapter.logger().info("Mirage reload completed with {} MOTD definition(s).", refreshed.size());
        } catch (Exception exception) {
            platformAdapter.logger().error("Mirage reload failed.", exception);
            renders.put(DEFAULT_MOTD_KEY, MotdRender.loading("Loading...", List.of()));
        }
    }

    public MotdRender motd(String key, int protocolVersion) {
        MirageConfig.MotdEntry motdEntry = config.motds().getOrDefault(key, config.motds().get(DEFAULT_MOTD_KEY));
        String fallbackText = motdEntry != null ? motdEntry.fallbackText() : "Mirage";
        if (!supportsModern(protocolVersion)) {
            return MotdRender.ready("", fallbackText);
        }

        MotdRender render = renders.get(key);
        if (render != null) {
            return render;
        }
        return renders.getOrDefault(DEFAULT_MOTD_KEY, MotdRender.loading("Loading...", List.of()));
    }

    public boolean supportsModern(int protocolVersion) {
        return protocolVersion >= config.settings().minimumModernProtocol();
    }

    @Override
    public synchronized void close() {
        closeCurrentResources();
    }

    private MotdRender renderMotd(
            MotdRenderService renderService,
            MirageConfig config,
            MirageConfig.MotdEntry motdEntry
    ) throws IOException, InterruptedException {
        if (!"image".equalsIgnoreCase(motdEntry.type())) {
            return MotdRender.ready("", motdEntry.fallbackText());
        }

        MirageConfig.ImageEntry imageEntry = config.images().get(motdEntry.targetImage());
        if (imageEntry == null) {
            platformAdapter.logger().warn("Mirage image '{}' is not defined.", motdEntry.targetImage());
            return MotdRender.ready("", motdEntry.fallbackText());
        }

        BufferedImage image = loadImage(platformAdapter.dataDirectory().resolve(imageEntry.file()));
        List<TileSkin> missingTiles = renderService.missingTiles(image);
        if (!missingTiles.isEmpty()) {
            if (mineskinClient instanceof NoopMineskinClient) {
                platformAdapter.logger().warn(
                        "Mirage found {} uncached tile(s) for '{}' but no Mineskin API key is configured.",
                        missingTiles.size(),
                        imageEntry.file()
                );
                return MotdRender.loading("Loading...", missingHashes(missingTiles));
            }

            for (TileSkin tile : missingTiles) {
                SkinData skinData = mineskinClient.upload(tile.tileHash(), tile.skinImage());
                skinCache.put(tile.tileHash(), skinData);
            }
        }

        return renderService.render(image, motdEntry.fallbackText());
    }

    private BufferedImage loadImage(Path imagePath) throws IOException {
        if (Files.notExists(imagePath)) {
            throw new IOException("Configured image does not exist: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("Failed to decode image: " + imagePath);
        }
        return image;
    }

    private void ensureDefaultConfig(Path configPath) throws IOException {
        if (Files.exists(configPath)) {
            return;
        }

        try (InputStream stream = MirageRuntime.class.getResourceAsStream("/config.yml")) {
            if (stream == null) {
                throw new IOException("Default config.yml resource is missing from the classpath.");
            }
            Files.copy(stream, configPath);
        }
    }

    private List<String> missingHashes(List<TileSkin> missingTiles) {
        List<String> hashes = new ArrayList<>(missingTiles.size());
        for (TileSkin tile : missingTiles) {
            hashes.add(tile.tileHash());
        }
        return hashes;
    }

    private void closeCurrentResources() {
        skinCache.close();
        if (mineskinClient instanceof AutoCloseable autoCloseable) {
            try {
                autoCloseable.close();
            } catch (Exception exception) {
                platformAdapter.logger().warn("Failed to close the Mineskin client cleanly.", exception);
            }
        }
        skinCache = new InMemorySkinCache();
        mineskinClient = new NoopMineskinClient();
    }
}
