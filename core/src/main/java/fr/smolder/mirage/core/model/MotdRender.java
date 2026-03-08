package fr.smolder.mirage.core.model;

import java.util.List;

public record MotdRender(
        RenderState state,
        String modernJson,
        String fallbackText,
        List<String> missingTileHashes
) {
    public MotdRender {
        missingTileHashes = List.copyOf(missingTileHashes);
    }

    public static MotdRender ready(String modernJson, String fallbackText) {
        return new MotdRender(RenderState.READY, modernJson, fallbackText, List.of());
    }

    public static MotdRender loading(String fallbackText, List<String> missingTileHashes) {
        return new MotdRender(RenderState.LOADING, "", fallbackText, missingTileHashes);
    }

    public enum RenderState {
        READY,
        LOADING
    }
}
