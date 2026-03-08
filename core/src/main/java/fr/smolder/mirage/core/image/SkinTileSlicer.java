package fr.smolder.mirage.core.image;

import fr.smolder.mirage.core.model.SlicedImage;
import fr.smolder.mirage.core.model.TileSkin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class SkinTileSlicer implements ImageSlicer {
    private static final int TILE_SIZE = 8;
    private static final int SKIN_SIZE = 64;
    private static final int FACE_OFFSET_X = 8;
    private static final int FACE_OFFSET_Y = 8;

    @Override
    public SlicedImage slice(BufferedImage image) {
        if (image.getWidth() % TILE_SIZE != 0 || image.getHeight() % TILE_SIZE != 0) {
            throw new IllegalArgumentException("Image dimensions must be divisible by 8.");
        }

        int columns = image.getWidth() / TILE_SIZE;
        int rows = image.getHeight() / TILE_SIZE;
        List<TileSkin> tiles = new ArrayList<>(columns * rows);

        for (int tileY = 0; tileY < rows; tileY++) {
            for (int tileX = 0; tileX < columns; tileX++) {
                int startX = tileX * TILE_SIZE;
                int startY = tileY * TILE_SIZE;
                BufferedImage skin = new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_ARGB);

                for (int localY = 0; localY < TILE_SIZE; localY++) {
                    for (int localX = 0; localX < TILE_SIZE; localX++) {
                        int source = image.getRGB(startX + localX, startY + localY);
                        skin.setRGB(FACE_OFFSET_X + localX, FACE_OFFSET_Y + localY, normalizePixel(source));
                    }
                }

                String tileHash = TileHasher.hashTile(image, startX, startY, TILE_SIZE);
                tiles.add(new TileSkin(tileHash, skin));
            }
        }

        return new SlicedImage(columns, rows, tiles);
    }

    private static int normalizePixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) {
            return 0xFF000000;
        }
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }
}
