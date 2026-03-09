package fr.smolder.mirage.core.cache;

import fr.smolder.mirage.core.model.SkinData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSkinCacheTest {
	@TempDir
	Path tempDir;

	@Test
	void persistsSkinDataAcrossCacheInstances() throws Exception {
		Path databasePath = tempDir.resolve("mirage-cache.sqlite");

		try (SqliteSkinCache firstCache = new SqliteSkinCache(databasePath)) {
			firstCache.put("tile-a", new SkinData("texture-value", "signature-value"));
		}

		try (SqliteSkinCache secondCache = new SqliteSkinCache(databasePath)) {
			SkinData restored = secondCache.get("tile-a").orElseThrow();
			assertEquals("texture-value", restored.textureBase64());
			assertEquals("signature-value", restored.signature());
			assertTrue(secondCache.get("missing").isEmpty());
		}
	}
}
