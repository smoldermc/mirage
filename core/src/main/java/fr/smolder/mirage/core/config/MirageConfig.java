package fr.smolder.mirage.core.config;

import java.util.LinkedHashMap;
import java.util.List;
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
		images.put("server_logo", new ImageEntry("logo.png", "#FFFFFF", "#FFFFFFFF", List.of()));

		Map<String, MotdEntry> motds = new LinkedHashMap<>();
		motds.put("default", new MotdEntry("image", "server_logo", "<red>Legacy clients see this!"));

		return new MirageConfig(
				new Settings("", "sqlite", 769, "unlisted"),
				images,
				motds
		);
	}

	public record Settings(
			String mineskinApiKey,
			String databaseType,
			int minimumModernProtocol,
			String mineskinSkinVisibility
	) {
	}

	public record ImageEntry(String file, String textColor, String shadowColor, List<LineStyle> lineStyles) {
		public ImageEntry {
			lineStyles = List.copyOf(lineStyles);
		}
	}

	public record LineStyle(String textColor, String shadowColor) {
	}

	public record MotdEntry(String type, String targetImage, String fallbackText) {
	}
}
