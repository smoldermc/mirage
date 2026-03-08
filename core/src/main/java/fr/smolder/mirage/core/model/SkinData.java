package fr.smolder.mirage.core.model;

import java.util.Objects;

public record SkinData(String textureBase64, String signature) {
    public SkinData {
        textureBase64 = TextureValueNormalizer.normalize(Objects.requireNonNull(textureBase64, "textureBase64"));
        signature = signature == null ? "" : signature;
    }
}
