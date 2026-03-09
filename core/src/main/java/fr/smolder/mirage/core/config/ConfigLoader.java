package fr.smolder.mirage.core.config;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {
	public MirageConfig load(Path path) throws IOException {
		if (Files.notExists(path)) {
			return MirageConfig.defaults();
		}

		YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
				.setPath(path)
				.build();
		ConfigurationNode root = loader.load();

		ConfigurationNode settingsNode = root.getNode("settings");
		MirageConfig.Settings settings = new MirageConfig.Settings(
				settingsNode.getNode("mineskin_api_key").getString(""),
				settingsNode.getNode("database_type").getString("sqlite"),
				settingsNode.getNode("minimum_modern_protocol").getInt(769),
				settingsNode.getNode("mineskin_skin_visibility").getString("unlisted")
		);

		Map<String, MirageConfig.ImageEntry> images = new LinkedHashMap<>();
		for (Map.Entry<Object, ? extends ConfigurationNode> child : root.getNode("images").getChildrenMap().entrySet()) {
			String key = String.valueOf(child.getKey());
			ConfigurationNode node = child.getValue();
			List<MirageConfig.LineStyle> lineStyles = new ArrayList<>();
			for (ConfigurationNode styleNode : node.getNode("line_styles").getChildrenList()) {
				lineStyles.add(new MirageConfig.LineStyle(
						styleNode.getNode("text_color").getString(),
						styleNode.getNode("shadow_color").getString()
				));
			}
			images.put(key, new MirageConfig.ImageEntry(
					node.getNode("file").getString(""),
					node.getNode("text_color").getString("#FFFFFF"),
					node.getNode("shadow_color").getString("#FFFFFFFF"),
					lineStyles
			));
		}

		Map<String, MirageConfig.MotdEntry> motds = new LinkedHashMap<>();
		for (Map.Entry<Object, ? extends ConfigurationNode> child : root.getNode("motd").getChildrenMap().entrySet()) {
			String key = String.valueOf(child.getKey());
			ConfigurationNode node = child.getValue();
			motds.put(key, new MirageConfig.MotdEntry(
					node.getNode("type").getString("image"),
					node.getNode("target_image").getString(""),
					node.getNode("fallback_text").getString("Loading...")
			));
		}

		return new MirageConfig(
				settings,
				images.isEmpty() ? MirageConfig.defaults().images() : images,
				motds.isEmpty() ? MirageConfig.defaults().motds() : motds
		);
	}
}
