package fr.smolder.mirage.core.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class TextureValueNormalizer {
	private TextureValueNormalizer() {
	}

	static String normalize(String encodedValue) {
		if (encodedValue == null || encodedValue.isBlank()) {
			return "";
		}

		try {
			String decoded = new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
			JsonElement parsed = JsonParser.parseString(decoded);
			if (!parsed.isJsonObject()) {
				return encodedValue;
			}

			JsonObject root = parsed.getAsJsonObject();
			JsonElement textures = root.get("textures");
			if (textures == null || textures.isJsonNull()) {
				return encodedValue;
			}

			JsonObject minimized = new JsonObject();
			minimized.add("textures", textures.deepCopy());
			return Base64.getEncoder().encodeToString(minimized.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IllegalArgumentException | JsonParseException ignored) {
			return encodedValue;
		}
	}
}
