package fr.smolder.mirage.core.text;

import fr.smolder.mirage.core.model.MotdRender;
import fr.smolder.mirage.core.model.SkinData;
import fr.smolder.mirage.core.model.SlicedImage;
import fr.smolder.mirage.core.model.TileSkin;

import java.util.List;
import java.util.Map;

public final class ObjectComponentJsonGenerator {
	private static final String EMPTY_PROFILE_ID = "00000000-0000-0000-0000-000000000000";

	public String generate(
			SlicedImage slicedImage,
			Map<String, SkinData> skinDataByHash,
			List<MotdRender.LineStyle> lineStyles
	) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\"text\":\"\",\"extra\":[");

		boolean first = true;
		for (int row = 0; row < slicedImage.rows(); row++) {
			if (!first) {
				builder.append(',');
			}
			first = false;
			builder.append(lineComponent(slicedImage, skinDataByHash, lineStyles, row));
			if (row + 1 < slicedImage.rows()) {
				builder.append(",{\"text\":\"\\n\"}");
			}
		}

		builder.append("]}");
		return builder.toString();
	}

	private static String lineComponent(
			SlicedImage slicedImage,
			Map<String, SkinData> skinDataByHash,
			List<MotdRender.LineStyle> lineStyles,
			int row
	) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\"text\":\"\"");
		if (row < lineStyles.size()) {
			MotdRender.LineStyle lineStyle = lineStyles.get(row);
			builder.append(",\"color\":\"").append(escape(lineStyle.textColor())).append('"');
			builder.append(",\"shadow_color\":\"").append(escape(lineStyle.shadowColor())).append('"');
		}
		builder.append(",\"extra\":[");

		int start = row * slicedImage.columns();
		int end = Math.min(start + slicedImage.columns(), slicedImage.tiles().size());
		for (int index = start; index < end; index++) {
			if (index > start) {
				builder.append(',');
			}
			TileSkin tile = slicedImage.tiles().get(index);
			SkinData skinData = skinDataByHash.get(tile.tileHash());
			if (skinData == null) {
				throw new IllegalArgumentException("Missing skin data for tile hash " + tile.tileHash());
			}
			builder.append(objectComponent(skinData, tile.hat()));
		}

		builder.append("]}");
		return builder.toString();
	}

	private static String objectComponent(SkinData skinData, boolean hat) {
		return """
				{"type":"object","player":{"id":"%s","properties":[{"name":"textures","value":"%s"}]},"hat":%s}
				""".formatted(
				EMPTY_PROFILE_ID,
				escape(skinData.textureBase64()),
				hat
		).trim();
	}

	private static String escape(String input) {
		return input
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}
}
