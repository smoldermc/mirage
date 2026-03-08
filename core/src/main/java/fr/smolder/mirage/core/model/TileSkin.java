package fr.smolder.mirage.core.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

public record TileSkin(String tileHash, BufferedImage skinImage) {
    public TileSkin {
        tileHash = Objects.requireNonNull(tileHash, "tileHash");
        skinImage = Objects.requireNonNull(skinImage, "skinImage");
    }
}
