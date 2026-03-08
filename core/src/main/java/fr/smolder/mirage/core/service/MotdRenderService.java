package fr.smolder.mirage.core.service;

import fr.smolder.mirage.core.cache.SkinCache;
import fr.smolder.mirage.core.image.ImageSlicer;
import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.RenderedSkin;
import fr.smolder.mirage.core.model.SkinData;
import fr.smolder.mirage.core.model.SlicedImage;
import fr.smolder.mirage.core.model.TileSkin;
import fr.smolder.mirage.core.text.ObjectComponentJsonGenerator;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public MotdRender render(BufferedImage image, String fallbackText, String textColor, String shadowColor) {
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
            return MotdRender.loading("Loading...", List.copyOf(missing), textColor, shadowColor);
        }

        List<RenderedSkin> orderedSkins = new ArrayList<>(slicedImage.tiles().size());
        for (TileSkin tile : slicedImage.tiles()) {
            orderedSkins.add(new RenderedSkin(resolved.get(tile.tileHash()), tile.hat()));
        }
        return MotdRender.ready(
                jsonGenerator.generate(slicedImage, resolved),
                fallbackText,
                slicedImage.columns(),
                orderedSkins,
                textColor,
                shadowColor
        );
    }
}
