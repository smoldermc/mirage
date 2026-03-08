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
            Path dataDirectory = platformAdapter.dataDirectory();
            Path configPath = dataDirectory.resolve("config.yml");
            Path cachePath = dataDirectory.resolve("mirage-cache.sqlite");
            Files.createDirectories(dataDirectory);
            ensureDefaultConfig(configPath);
            platformAdapter.logger().info("Reloading Mirage from {}.", dataDirectory);

            MirageConfig loadedConfig = configLoader.load(configPath);
            closeCurrentResources();

            this.config = loadedConfig;
            this.skinCache = new SqliteSkinCache(cachePath);
            this.mineskinClient = loadedConfig.settings().mineskinApiKey().isBlank()
                    ? new NoopMineskinClient()
                    : new RealMineskinClient(
                            loadedConfig.settings().mineskinApiKey(),
                            loadedConfig.settings().mineskinSkinVisibility(),
                            platformAdapter.logger()
                    );
            platformAdapter.logger().info(
                    "Mirage config loaded: images={}, motds={}, modernProtocol>={}, mineskinApiKeyConfigured={}, mineskinVisibility={}.",
                    loadedConfig.images().size(),
                    loadedConfig.motds().size(),
                    loadedConfig.settings().minimumModernProtocol(),
                    !loadedConfig.settings().mineskinApiKey().isBlank(),
                    loadedConfig.settings().mineskinSkinVisibility()
            );

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
            platformAdapter.logger().info(
                    "Mirage reload completed with {} MOTD definition(s): {}.",
                    refreshed.size(),
                    summarizeRenders(refreshed)
            );
        } catch (Exception exception) {
            platformAdapter.logger().error("Mirage reload failed.", exception);
            renders.put(DEFAULT_MOTD_KEY, MotdRender.loading("Loading...", List.of()));
        }
    }

    public MotdRender motd(String key, int protocolVersion) {
        MirageConfig.MotdEntry motdEntry = config.motds().getOrDefault(key, config.motds().get(DEFAULT_MOTD_KEY));
        String fallbackText = motdEntry != null ? motdEntry.fallbackText() : "Mirage";
        if (!supportsModern(protocolVersion)) {
            platformAdapter.logger().debug(
                    "Serving fallback MOTD for key '{}' because client protocol {} is below {}.",
                    key,
                    protocolVersion,
                    config.settings().minimumModernProtocol()
            );
            return MotdRender.ready("", fallbackText);
        }

        MotdRender render = renders.get(key);
        if (render != null) {
            if (render.state() == MotdRender.RenderState.LOADING) {
                platformAdapter.logger().debug(
                        "Serving loading MOTD for key '{}' with {} unresolved tile(s): {}.",
                        key,
                        render.missingTileHashes().size(),
                        summarizeHashes(render.missingTileHashes())
                );
            }
            return render;
        }
        platformAdapter.logger().warn("No MOTD render found for key '{}', serving default loading state.", key);
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
            platformAdapter.logger().info(
                    "MOTD '{}' is non-image type '{}', serving fallback text only.",
                    motdEntry.targetImage(),
                    motdEntry.type()
            );
            return MotdRender.ready("", motdEntry.fallbackText());
        }

        MirageConfig.ImageEntry imageEntry = config.images().get(motdEntry.targetImage());
        if (imageEntry == null) {
            platformAdapter.logger().warn("Mirage image '{}' is not defined.", motdEntry.targetImage());
            return MotdRender.ready("", motdEntry.fallbackText());
        }

        Path imagePath = platformAdapter.dataDirectory().resolve(imageEntry.file());
        BufferedImage image = loadImage(imagePath);
        platformAdapter.logger().info(
                "Rendering MOTD image '{}' from {} ({}x{}).",
                motdEntry.targetImage(),
                imagePath,
                image.getWidth(),
                image.getHeight()
        );
        List<TileSkin> missingTiles = renderService.missingTiles(image);
        int totalTiles = renderService.slice(image).tiles().size();
        int cachedTiles = totalTiles - missingTiles.size();
        platformAdapter.logger().info(
                "MOTD '{}' resolved {} cached tile(s) and {} missing tile(s) out of {}.",
                motdEntry.targetImage(),
                cachedTiles,
                missingTiles.size(),
                totalTiles
        );
        if (!missingTiles.isEmpty()) {
            if (mineskinClient instanceof NoopMineskinClient) {
                platformAdapter.logger().warn(
                        "Mirage found {} uncached tile(s) for '{}' but no Mineskin API key is configured. Missing hashes: {}.",
                        missingTiles.size(),
                        imageEntry.file(),
                        summarizeTileHashes(missingTiles)
                );
                return MotdRender.loading("Loading...", missingHashes(missingTiles));
            }

            int index = 0;
            for (TileSkin tile : missingTiles) {
                index++;
                platformAdapter.logger().info(
                        "Uploading missing tile {}/{} for '{}' ({})",
                        index,
                        missingTiles.size(),
                        motdEntry.targetImage(),
                        tile.tileHash()
                );
                SkinData skinData = mineskinClient.upload(tile.tileHash(), tile.skinImage());
                skinCache.put(tile.tileHash(), skinData);
            }
            platformAdapter.logger().info(
                    "Finished uploading {} missing tile(s) for '{}'.",
                    missingTiles.size(),
                    motdEntry.targetImage()
            );
        }

        MotdRender render = renderService.render(image, motdEntry.fallbackText());
        if (render.state() == MotdRender.RenderState.READY) {
            platformAdapter.logger().info(
                    "MOTD '{}' is ready with {} component tile(s).",
                    motdEntry.targetImage(),
                    totalTiles
            );
        } else {
            platformAdapter.logger().warn(
                    "MOTD '{}' is still loading with {} unresolved tile(s): {}.",
                    motdEntry.targetImage(),
                    render.missingTileHashes().size(),
                    summarizeHashes(render.missingTileHashes())
            );
        }
        return render;
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

    private String summarizeRenders(Map<String, MotdRender> refreshed) {
        List<String> parts = new ArrayList<>(refreshed.size());
        for (var entry : refreshed.entrySet()) {
            MotdRender render = entry.getValue();
            parts.add(entry.getKey() + "=" + render.state() + "(" + render.missingTileHashes().size() + " missing)");
        }
        return String.join(", ", parts);
    }

    private String summarizeTileHashes(List<TileSkin> tiles) {
        List<String> hashes = new ArrayList<>(tiles.size());
        for (TileSkin tile : tiles) {
            hashes.add(tile.tileHash());
        }
        return summarizeHashes(hashes);
    }

    private String summarizeHashes(List<String> hashes) {
        if (hashes.isEmpty()) {
            return "none";
        }
        int limit = Math.min(5, hashes.size());
        String summary = String.join(", ", hashes.subList(0, limit));
        if (hashes.size() > limit) {
            return summary + " ... +" + (hashes.size() - limit) + " more";
        }
        return summary;
    }
}
