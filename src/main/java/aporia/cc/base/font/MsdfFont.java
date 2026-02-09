package aporia.cc.base.font;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import aporia.cc.Aporia;
import aporia.cc.utility.interfaces.IMinecraft;
import aporia.cc.utility.render.display.base.Gradient;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsdfFont implements IMinecraft {

	@Getter
    private final String name;
	@Getter
	private final Identifier atlasIdentifier;
	private final AbstractTexture texture;
	@Getter
    private final FontData.AtlasData atlas;
	@Getter
    private final FontData.MetricsData metrics;
	private final Map<Integer, MsdfGlyph> glyphs;
	private final Map<Integer, Map<Integer, Float>> kernings;

	private MsdfFont(String name, Identifier atlasIdentifier, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
		this.name = name;
		this.atlasIdentifier = atlasIdentifier;
		this.texture = texture;
		this.atlas = atlas;
		this.metrics = metrics;
		this.glyphs = glyphs;
		this.kernings = kernings;
	}

	public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
        texture.setFilter(true, true);
		text = text.replace("ᴀ", "A")
				.replace("ʙ", "B")
				.replace("ᴄ", "C")
				.replace("ᴅ", "D")
				.replace("ᴇ", "E")
				.replace("ꜰ", "F")
				.replace("ɢ", "G")
				.replace("ʜ", "H")
				.replace("ɪ", "I")
				.replace("ᴊ", "J")
				.replace("ᴋ", "K")
				.replace("ʟ", "L")
				.replace("ᴍ", "M")
				.replace("ɴ", "N")
				.replace("ᴏ", "O")
				.replace("ᴘ", "P")
				.replace("ʀ", "R")
				.replace("ꜱ", "S")
				.replace("ᴛ", "T")
				.replace("ᴜ", "U")
				.replace("ᴠ", "V")
				.replace("ᴡ", "W")
				.replace("ʏ", "Y")
				.replace("ᴢ", "Z")
				.replace("ǫ", "Q")
				.replace("ʠ", "Q");
		int prevChar = -1;
		boolean skipNext = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(c=='ᴀ'){
				c='А';

			}
			if (skipNext) {
				skipNext = false;
				continue;
			}

			if (c == '§') {
				skipNext = true;
				continue;
			}

			MsdfGlyph glyph = this.glyphs.get((int) c);

			if (glyph == null) continue;

			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault((int) c, 0.0f) * size;
			}

			x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
			prevChar = c;
		}
	}
	public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, Gradient color) {
        texture.setFilter(true, true);
		text = text.replace("ᴀ", "A")
				.replace("ʙ", "B")
				.replace("ᴄ", "C")
				.replace("ᴅ", "D")
				.replace("ᴇ", "E")
				.replace("ꜰ", "F")
				.replace("ɢ", "G")
				.replace("ʜ", "H")
				.replace("ɪ", "I")
				.replace("ᴊ", "J")
				.replace("ᴋ", "K")
				.replace("ʟ", "L")
				.replace("ᴍ", "M")
				.replace("ɴ", "N")
				.replace("ᴏ", "O")
				.replace("ᴘ", "P")
				.replace("ʀ", "R")
				.replace("ꜱ", "S")
				.replace("ᴛ", "T")
				.replace("ᴜ", "U")
				.replace("ᴠ", "V")
				.replace("ᴡ", "W")
				.replace("ʏ", "Y")
				.replace("ᴢ", "Z")
				.replace("ǫ", "Q")
				.replace("ʠ", "Q");
		int prevChar = -1;
		boolean skipNext = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (skipNext) {
				skipNext = false;
				continue;
			}

			if (c == '§') {
				skipNext = true;
				continue;
			}

			MsdfGlyph glyph = this.glyphs.get((int) c);

			if (glyph == null) continue;

			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault((int) c, 0.0f) * size;
			}

			x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
			prevChar = c;
		}
	}
public float getWidth(String text, float size) {
	text = text.replace("ᴀ", "A")
			.replace("ʙ", "B")
			.replace("ᴄ", "C")
			.replace("ᴅ", "D")
			.replace("ᴇ", "E")
			.replace("ꜰ", "F")
			.replace("ɢ", "G")
			.replace("ʜ", "H")
			.replace("ɪ", "I")
			.replace("ᴊ", "J")
			.replace("ᴋ", "K")
			.replace("ʟ", "L")
			.replace("ᴍ", "M")
			.replace("ɴ", "N")
			.replace("ᴏ", "O")
			.replace("ᴘ", "P")
			.replace("ʀ", "R")
			.replace("ꜱ", "S")
			.replace("ᴛ", "T")
			.replace("ᴜ", "U")
			.replace("ᴠ", "V")
			.replace("ᴡ", "W")
			.replace("ʏ", "Y")
			.replace("ᴢ", "Z")
			.replace("ǫ", "Q")
			.replace("ʠ", "Q");
	int prevChar = -1;
    float width = 0.0f;
    boolean skipNext = false;


    for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
		if(c=='ᴀ'){
			c='А';

		}
        if (skipNext) {
            skipNext = false;
            continue;
        }

        if (c == '§') {
            skipNext = true;
            continue;
        }

        MsdfGlyph glyph = this.glyphs.get((int) c);

        if (glyph == null) continue;

        Map<Integer, Float> kerning = this.kernings.get(prevChar);
        if (kerning != null) {
            width += kerning.getOrDefault((int) c, 0.0f) * size;
        }

        width += glyph.getWidth(size) ;
        prevChar = (int) c;
    }

    return width;
}

	public float getTextWidth(Text text, float size) {
		return getWidth(text.getString(), size);
	}

	public Font getFont(float size) {
		return new Font(this, size);
	}

    public static MsdfFont.Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name = "?";
		private Identifier dataIdentifer;
		private Identifier atlasIdentifier;

		private Builder() {
		}

		public MsdfFont.Builder name(String name) {
			this.name = name;
			return this;
		}

		public MsdfFont.Builder data(String dataFileName) {
			this.dataIdentifer = Aporia.id("fonts/msdf/" + dataFileName + ".json");
			return this;
		}

		public MsdfFont.Builder atlas(String atlasFileName) {
			this.atlasIdentifier = Aporia.id("fonts/msdf/" + atlasFileName + ".png");
			return this;
		}

		public MsdfFont build() {
			FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
			AbstractTexture texture = mc.getTextureManager().getTexture(this.atlasIdentifier);

			if (data == null) {
				throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() +
						"; Are you sure this is json file? Try to check the correctness of its syntax.");
			}

			RenderSystem.queueFencedTask(() -> texture.setFilter(true, true));

			float aWidth = data.atlas().width();
			float aHeight = data.atlas().height();
			Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
					.collect(Collectors.<FontData.GlyphData, Integer, MsdfGlyph>toMap(
                            FontData.GlyphData::unicode,
							(glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight)
					));

			Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
			data.kernings().forEach((kerning) -> {
                Map<Integer, Float> map = kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>());

                map.put(kerning.rightChar(), kerning.advance());
			});

			return new MsdfFont(this.name, this.atlasIdentifier, texture, data.atlas(), data.metrics(), glyphs, kernings);
		}

	}
}

