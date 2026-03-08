package fr.smolder.mirage.core.model;

import java.util.Objects;

public record RenderedSkin(SkinData skinData, boolean hat) {
    public RenderedSkin {
        skinData = Objects.requireNonNull(skinData, "skinData");
    }
}
