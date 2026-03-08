package fr.smolder.mirage.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

public record MirageConfig(
        Settings settings,
        Map<String, ImageEntry> images,
        Map<String, MotdEntry> motds
) {
    public MirageConfig {
        images = Map.copyOf(images);
        motds = Map.copyOf(motds);
    }

    public static MirageConfig defaults() {
        Map<String, ImageEntry> images = new LinkedHashMap<>();
        images.put("server_logo", new ImageEntry("logo.png", "#00000000"));

        Map<String, MotdEntry> motds = new LinkedHashMap<>();
        motds.put("default", new MotdEntry("image", "server_logo", "<red>Legacy clients see this!"));

        return new MirageConfig(
                new Settings("", "sqlite", 769),
                images,
                motds
        );
    }

    public record Settings(String mineskinApiKey, String databaseType, int minimumModernProtocol) {
    }

    public record ImageEntry(String file, String shadowColor) {
    }

    public record MotdEntry(String type, String targetImage, String fallbackText) {
    }
}
