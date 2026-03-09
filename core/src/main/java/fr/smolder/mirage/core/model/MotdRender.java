package fr.smolder.mirage.core.model;

import java.util.List;

public record MotdRender(
		RenderState state,
		String modernJson,
		String fallbackText,
		List<String> missingTileHashes,
		int columns,
		List<RenderedSkin> orderedSkins,
		List<LineStyle> lineStyles
) {
	public MotdRender {
		missingTileHashes = List.copyOf(missingTileHashes);
		orderedSkins = List.copyOf(orderedSkins);
		lineStyles = List.copyOf(lineStyles);
	}

	public static MotdRender ready(String modernJson, String fallbackText, int columns, List<RenderedSkin> orderedSkins) {
		return ready(modernJson, fallbackText, columns, orderedSkins, List.of());
	}

	public static MotdRender ready(
			String modernJson,
			String fallbackText,
			int columns,
			List<RenderedSkin> orderedSkins,
			List<LineStyle> lineStyles
	) {
		return new MotdRender(RenderState.READY, modernJson, fallbackText, List.of(), columns, orderedSkins, lineStyles);
	}

	public static MotdRender loading(String fallbackText, List<String> missingTileHashes) {
		return new MotdRender(RenderState.LOADING, "", fallbackText, missingTileHashes, 0, List.of(), List.of());
	}

	public record LineStyle(String textColor, String shadowColor) {
	}

	public enum RenderState {
		READY,
		LOADING
	}
}
