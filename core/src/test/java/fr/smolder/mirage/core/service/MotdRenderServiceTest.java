package fr.smolder.mirage.core.service;

import fr.smolder.mirage.core.cache.InMemorySkinCache;
import fr.smolder.mirage.core.image.SkinTileSlicer;
import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.SkinData;
import fr.smolder.mirage.core.model.TileSkin;
import fr.smolder.mirage.core.text.ObjectComponentJsonGenerator;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotdRenderServiceTest {
	@Test
	void missingTilesDedupesPairedBaseAndHatSlices() {
		MotdRenderService service = new MotdRenderService(
				new SkinTileSlicer(),
				new InMemorySkinCache(),
				new ObjectComponentJsonGenerator()
		);

		List<TileSkin> missingTiles = service.missingTiles(twoTileImage());

		assertEquals(1, missingTiles.size());
		assertFalse(missingTiles.getFirst().hat());
	}

	@Test
	void renderPreservesHatFlagForPairedSlices() {
		InMemorySkinCache skinCache = new InMemorySkinCache();
		MotdRenderService service = new MotdRenderService(
				new SkinTileSlicer(),
				skinCache,
				new ObjectComponentJsonGenerator()
		);
		TileSkin tileSkin = service.slice(twoTileImage()).tiles().getFirst();
		skinCache.put(tileSkin.tileHash(), new SkinData("texture", "signature"));

		MotdRender render = service.render(
				twoTileImage(),
				"Fallback",
				"#FFFFFF",
				"#FFFFFFFF",
				List.of(new MotdRender.LineStyle(null, "#FF0000FF"))
		);

		assertEquals(MotdRender.RenderState.READY, render.state());
		assertEquals(2, render.orderedSkins().size());
		assertEquals(1, render.lineStyles().size());
		assertFalse(render.orderedSkins().get(0).hat());
		assertTrue(render.orderedSkins().get(1).hat());
		assertEquals("#FFFFFF", render.lineStyles().getFirst().textColor());
		assertEquals("#FF0000FF", render.lineStyles().getFirst().shadowColor());
		assertTrue(render.modernJson().contains("\"hat\":false"));
		assertTrue(render.modernJson().contains("\"hat\":true"));
		assertTrue(render.modernJson().contains("\"shadow_color\":\"#FF0000FF\""));
		assertFalse(render.modernJson().contains("\"signature\""));
	}

	private static BufferedImage twoTileImage() {
		BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, 0xFFFF0000);
		image.setRGB(8, 0, 0xFF00FF00);
		return image;
	}
}
