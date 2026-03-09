package fr.smolder.mirage.core.service;

import fr.smolder.mirage.core.cache.SkinCache;
import fr.smolder.mirage.core.image.ImageSlicer;
import fr.smolder.mirage.core.model.*;
import fr.smolder.mirage.core.text.ObjectComponentJsonGenerator;

import java.awt.image.BufferedImage;
import java.util.*;

public final class MotdRenderService {
	private final ImageSlicer imageSlicer;
	private final SkinCache skinCache;
	private final ObjectComponentJsonGenerator jsonGenerator;

	public MotdRenderService(
			ImageSlicer imageSlicer,
			SkinCache skinCache,
			ObjectComponentJsonGenerator jsonGenerator
	) {
		this.imageSlicer = imageSlicer;
		this.skinCache = skinCache;
		this.jsonGenerator = jsonGenerator;
	}

	public SlicedImage slice(BufferedImage image) {
		return imageSlicer.slice(image);
	}

	public List<TileSkin> missingTiles(BufferedImage image) {
		SlicedImage slicedImage = imageSlicer.slice(image);
		List<TileSkin> missing = new ArrayList<>();
		Set<String> seenHashes = new LinkedHashSet<>();

		for (TileSkin tile : slicedImage.tiles()) {
			if (seenHashes.add(tile.tileHash()) && skinCache.get(tile.tileHash()).isEmpty()) {
				missing.add(tile);
			}
		}

		return missing;
	}

	public void cache(String tileHash, SkinData skinData) {
		skinCache.put(tileHash, skinData);
	}

	public MotdRender render(
			BufferedImage image,
			String fallbackText,
			String textColor,
			String shadowColor,
			List<MotdRender.LineStyle> lineStyleOverrides
	) {
		SlicedImage slicedImage = imageSlicer.slice(image);
		Map<String, SkinData> resolved = new LinkedHashMap<>();
		Set<String> missing = new LinkedHashSet<>();

		for (TileSkin tile : slicedImage.tiles()) {
			skinCache.get(tile.tileHash()).ifPresentOrElse(
					skinData -> resolved.put(tile.tileHash(), skinData),
					() -> missing.add(tile.tileHash())
			);
		}

		if (!missing.isEmpty()) {
			return MotdRender.loading("Loading...", List.copyOf(missing));
		}

		List<MotdRender.LineStyle> resolvedLineStyles = resolveLineStyles(
				slicedImage.rows(),
				textColor,
				shadowColor,
				lineStyleOverrides
		);
		List<RenderedSkin> orderedSkins = new ArrayList<>(slicedImage.tiles().size());
		for (TileSkin tile : slicedImage.tiles()) {
			orderedSkins.add(new RenderedSkin(resolved.get(tile.tileHash()), tile.hat()));
		}
		return MotdRender.ready(
				jsonGenerator.generate(slicedImage, resolved, resolvedLineStyles),
				fallbackText,
				slicedImage.columns(),
				orderedSkins,
				resolvedLineStyles
		);
	}

	private static List<MotdRender.LineStyle> resolveLineStyles(
			int rows,
			String defaultTextColor,
			String defaultShadowColor,
			List<MotdRender.LineStyle> lineStyleOverrides
	) {
		List<MotdRender.LineStyle> resolved = new ArrayList<>(rows);
		for (int row = 0; row < rows; row++) {
			MotdRender.LineStyle override = row < lineStyleOverrides.size() ? lineStyleOverrides.get(row) : null;
			resolved.add(new MotdRender.LineStyle(
					overrideColor(override != null ? override.textColor() : null, defaultTextColor),
					overrideColor(override != null ? override.shadowColor() : null, defaultShadowColor)
			));
		}
		return resolved;
	}

	private static String overrideColor(String override, String fallback) {
		return override == null || override.isBlank() ? fallback : override;
	}
}
