package fr.smolder.mirage.core.image;

import fr.smolder.mirage.core.model.SlicedImage;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkinTileSlicerTest {
    private final SkinTileSlicer slicer = new SkinTileSlicer();

    @Test
    void sliceBuildsOneSkinPerEightByEightTile() {
        BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        image.setRGB(8, 0, 0xFF00FF00);

        SlicedImage sliced = slicer.slice(image);

        assertEquals(2, sliced.columns());
        assertEquals(1, sliced.rows());
        assertEquals(2, sliced.tiles().size());
        assertEquals(0xFFFF0000, sliced.tiles().get(0).skinImage().getRGB(8, 8));
        assertEquals(0xFF00FF00, sliced.tiles().get(1).skinImage().getRGB(8, 8));
    }

    @Test
    void sliceRejectsNonAlignedImages() {
        BufferedImage image = new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
        assertThrows(IllegalArgumentException.class, () -> slicer.slice(image));
    }
}
