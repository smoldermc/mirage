package fr.smolder.mirage.core.model;

import java.util.List;

public record MotdRender(
        RenderState state,
        String modernJson,
        String fallbackText,
        List<String> missingTileHashes,
        int columns,
        List<RenderedSkin> orderedSkins,
        String textColor,
        String shadowColor
) {
    public static final String DEFAULT_TEXT_COLOR = "#FFFFFF";
    public static final String DEFAULT_SHADOW_COLOR = "#FFFFFFFF";

    public MotdRender {
        missingTileHashes = List.copyOf(missingTileHashes);
        orderedSkins = List.copyOf(orderedSkins);
    }

    public static MotdRender ready(String modernJson, String fallbackText, int columns, List<RenderedSkin> orderedSkins) {
        return ready(modernJson, fallbackText, columns, orderedSkins, DEFAULT_TEXT_COLOR, DEFAULT_SHADOW_COLOR);
    }

    public static MotdRender ready(
            String modernJson,
            String fallbackText,
            int columns,
            List<RenderedSkin> orderedSkins,
            String textColor,
            String shadowColor
    ) {
        return new MotdRender(RenderState.READY, modernJson, fallbackText, List.of(), columns, orderedSkins, textColor, shadowColor);
    }

    public static MotdRender loading(String fallbackText, List<String> missingTileHashes) {
        return loading(fallbackText, missingTileHashes, DEFAULT_TEXT_COLOR, DEFAULT_SHADOW_COLOR);
    }

    public static MotdRender loading(
            String fallbackText,
            List<String> missingTileHashes,
            String textColor,
            String shadowColor
    ) {
        return new MotdRender(RenderState.LOADING, "", fallbackText, missingTileHashes, 0, List.of(), textColor, shadowColor);
    }

    public enum RenderState {
        READY,
        LOADING
    }
}
