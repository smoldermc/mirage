package fr.smolder.mirage.core.image;

import fr.smolder.mirage.core.model.SlicedImage;
import fr.smolder.mirage.core.model.TileSkin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class SkinTileSlicer implements ImageSlicer {
	private static final int TILE_SIZE = 8;
	private static final int SKIN_SIZE = 64;
	private static final int BASE_OFFSET_X = 8;
	private static final int BASE_OFFSET_Y = 8;
	private static final int HAT_OFFSET_X = 40;
	private static final int HAT_OFFSET_Y = 8;

	@Override
	public SlicedImage slice(BufferedImage image) {
		if (image.getWidth() % TILE_SIZE != 0 || image.getHeight() % TILE_SIZE != 0) {
			throw new IllegalArgumentException("Image dimensions must be divisible by 8.");
		}

		int columns = image.getWidth() / TILE_SIZE;
		int rows = image.getHeight() / TILE_SIZE;
		int totalTiles = columns * rows;
		List<TileSkin> tiles = new ArrayList<>(columns * rows);

		for (int tileIndex = 0; tileIndex < totalTiles; tileIndex += 2) {
			TilePosition first = tilePosition(tileIndex, columns);
			TilePosition second = tileIndex + 1 < totalTiles ? tilePosition(tileIndex + 1, columns) : null;
			BufferedImage skin = new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_ARGB);

			paintTile(image, skin, first.startX(), first.startY(), BASE_OFFSET_X, BASE_OFFSET_Y);
			if (second != null) {
				paintTile(image, skin, second.startX(), second.startY(), HAT_OFFSET_X, HAT_OFFSET_Y);
			}

			String tileHash = TileHasher.hashTilePair(
					image,
					first.startX(),
					first.startY(),
					TILE_SIZE,
					second != null ? second.startX() : null,
					second != null ? second.startY() : null
			);
			tiles.add(new TileSkin(tileHash, skin, false));
			if (second != null) {
				tiles.add(new TileSkin(tileHash, skin, true));
			}
		}

		return new SlicedImage(columns, rows, tiles);
	}

	private static TilePosition tilePosition(int tileIndex, int columns) {
		int tileX = tileIndex % columns;
		int tileY = tileIndex / columns;
		return new TilePosition(tileX * TILE_SIZE, tileY * TILE_SIZE);
	}

	private static void paintTile(
			BufferedImage sourceImage,
			BufferedImage targetSkin,
			int startX,
			int startY,
			int targetOffsetX,
			int targetOffsetY
	) {
		for (int localY = 0; localY < TILE_SIZE; localY++) {
			for (int localX = 0; localX < TILE_SIZE; localX++) {
				int source = sourceImage.getRGB(startX + localX, startY + localY);
				targetSkin.setRGB(targetOffsetX + localX, targetOffsetY + localY, normalizePixel(source));
			}
		}
	}

	private static int normalizePixel(int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha == 0) {
			return 0xFF000000;
		}
		return 0xFF000000 | (argb & 0x00FFFFFF);
	}

	private record TilePosition(int startX, int startY) {
	}
}
