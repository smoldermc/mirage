package fr.smolder.mirage.core.model;

import java.util.List;

public record SlicedImage(int columns, int rows, List<TileSkin> tiles) {
	public SlicedImage {
		if (columns <= 0 || rows <= 0) {
			throw new IllegalArgumentException("columns and rows must be positive.");
		}
		if (tiles.size() != columns * rows) {
			throw new IllegalArgumentException("Tile count must match the image dimensions.");
		}
		tiles = List.copyOf(tiles);
	}
}
