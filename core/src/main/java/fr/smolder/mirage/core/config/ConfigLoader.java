package fr.smolder.mirage.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigLoader {
    public MirageConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            return MirageConfig.defaults();
        }

        ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder()
                .path(path)
                .build();
        CommentedConfigurationNode root = loader.load();

        CommentedConfigurationNode settingsNode = root.node("settings");
        MirageConfig.Settings settings = new MirageConfig.Settings(
                settingsNode.node("mineskin_api_key").getString(""),
                settingsNode.node("database_type").getString("sqlite"),
                settingsNode.node("minimum_modern_protocol").getInt(769)
        );

        Map<String, MirageConfig.ImageEntry> images = new LinkedHashMap<>();
        for (var child : root.node("images").childrenMap().entrySet()) {
            String key = String.valueOf(child.getKey());
            CommentedConfigurationNode node = child.getValue();
            images.put(key, new MirageConfig.ImageEntry(
                    node.node("file").getString(""),
                    node.node("shadow_color").getString("#00000000")
            ));
        }

        Map<String, MirageConfig.MotdEntry> motds = new LinkedHashMap<>();
        for (var child : root.node("motd").childrenMap().entrySet()) {
            String key = String.valueOf(child.getKey());
            CommentedConfigurationNode node = child.getValue();
            motds.put(key, new MirageConfig.MotdEntry(
                    node.node("type").getString("image"),
                    node.node("target_image").getString(""),
                    node.node("fallback_text").getString("Loading...")
            ));
        }

        return new MirageConfig(
                settings,
                images.isEmpty() ? MirageConfig.defaults().images() : images,
                motds.isEmpty() ? MirageConfig.defaults().motds() : motds
        );
    }
}
