package fr.smolder.mirage.core.image;

import fr.smolder.mirage.core.model.SlicedImage;

import java.awt.image.BufferedImage;

public interface ImageSlicer {
    SlicedImage slice(BufferedImage image);
}
