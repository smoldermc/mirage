package fr.smolder.mirage.core.image;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TileHasher {
	private TileHasher() {
	}

	public static String hashTile(BufferedImage image, int startX, int startY, int tileSize) {
		MessageDigest digest = md5();
		updateTile(digest, image, startX, startY, tileSize);
		return hex(digest.digest());
	}

	public static String hashTilePair(
			BufferedImage image,
			int firstStartX,
			int firstStartY,
			int tileSize,
			Integer secondStartX,
			Integer secondStartY
	) {
		MessageDigest digest = md5();
		updateTile(digest, image, firstStartX, firstStartY, tileSize);
		digest.update((byte) 0x7F);
		if (secondStartX != null && secondStartY != null) {
			updateTile(digest, image, secondStartX, secondStartY, tileSize);
		} else {
			digest.update((byte) 0x00);
		}
		return hex(digest.digest());
	}

	private static void updateTile(MessageDigest digest, BufferedImage image, int startX, int startY, int tileSize) {
		for (int y = 0; y < tileSize; y++) {
			for (int x = 0; x < tileSize; x++) {
				int normalized = normalizePixel(image.getRGB(startX + x, startY + y));
				digest.update((byte) (normalized >>> 24));
				digest.update((byte) (normalized >>> 16));
				digest.update((byte) (normalized >>> 8));
				digest.update((byte) normalized);
			}
		}
	}

	private static MessageDigest md5() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("MD5 is not available in this JVM.", exception);
		}
	}

	private static int normalizePixel(int argb) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha == 0) {
			return 0xFF000000;
		}
		return 0xFF000000 | (argb & 0x00FFFFFF);
	}

	private static String hex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			builder.append(Character.forDigit((value >>> 4) & 0x0F, 16));
			builder.append(Character.forDigit(value & 0x0F, 16));
		}
		return builder.toString();
	}
}
