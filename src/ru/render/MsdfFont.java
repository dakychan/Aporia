package ru.render;

import com.ferra13671.gltextureutils.GLTexture;
import com.ferra13671.gltextureutils.PathMode;
import com.ferra13671.gltextureutils.TextureFiltering;
import com.ferra13671.gltextureutils.TextureWrapping;
import com.ferra13671.gltextureutils.atlas.TextureAtlas;
import com.ferra13671.gltextureutils.atlas.TextureBorder;
import com.ferra13671.gltextureutils.loader.FileEntry;
import com.ferra13671.gltextureutils.loader.TextureLoaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MsdfFont {
   private final Map<Integer, MsdfFont.Glyph> glyphs;
   private final Map<Long, Float> kerning;
   private final GLTexture texture;
   private final TextureAtlas atlas;
   private final float distanceRange;
   private final float emSize;
   private final float lineHeight;
   private final float ascender;
   private final float descender;

   public MsdfFont(String jsonPath, String texturePath) {
      try {
         String json = this.readJsonFile(jsonPath);
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         JsonObject atlasObj = root.getAsJsonObject("atlas");
         int atlasWidth = atlasObj.get("width").getAsInt();
         int atlasHeight = atlasObj.get("height").getAsInt();
         this.distanceRange = atlasObj.has("distanceRange") ? atlasObj.get("distanceRange").getAsFloat() : 6.0F;
         JsonObject metrics = root.getAsJsonObject("metrics");
         this.emSize = metrics.has("emSize") ? metrics.get("emSize").getAsFloat() : 1.0F;
         this.lineHeight = metrics.has("lineHeight") ? metrics.get("lineHeight").getAsFloat() : this.emSize;
         this.ascender = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() : this.lineHeight;
         float descenderValue = metrics.has("descender") ? metrics.get("descender").getAsFloat() : 0.0F;
         this.descender = Math.abs(descenderValue);
         FileEntry fontTextureEntry = new FileEntry(texturePath, PathMode.INSIDE_JAR);
         GLTexture fontTexture = TextureLoaders.FILE_ENTRY
            .createTextureBuilder()
            .name("msdf-font-" + System.nanoTime())
            .info(fontTextureEntry)
            .filtering(TextureFiltering.SMOOTH)
            .wrapping(TextureWrapping.DEFAULT)
            .build();
         this.atlas = new TextureAtlas("msdf-font-atlas", Arrays.asList(fontTexture));
         this.texture = fontTexture;
         this.glyphs = new HashMap<>();
         JsonArray glyphArray = root.getAsJsonArray("glyphs");
         if (glyphArray != null) {
            TextureBorder border = this.atlas.getBorder(fontTexture);
            float borderU1 = border.getU1();
            float borderV1 = border.getV1();
            float borderU2 = border.getU2();
            float borderV2 = border.getV2();

            for (JsonElement element : glyphArray) {
               JsonObject glyphObj = element.getAsJsonObject();
               int codepoint = glyphObj.get("unicode").getAsInt();
               float advance = glyphObj.has("advance") ? glyphObj.get("advance").getAsFloat() : 0.0F;
               JsonObject planeBounds = glyphObj.has("planeBounds") ? glyphObj.getAsJsonObject("planeBounds") : null;
               JsonObject atlasBounds = glyphObj.has("atlasBounds") ? glyphObj.getAsJsonObject("atlasBounds") : null;
               MsdfFont.Glyph glyph;
               if (planeBounds != null && atlasBounds != null) {
                  float planeLeft = planeBounds.get("left").getAsFloat();
                  float planeBottom = planeBounds.get("bottom").getAsFloat();
                  float planeRight = planeBounds.get("right").getAsFloat();
                  float planeTop = planeBounds.get("top").getAsFloat();
                  float atlasLeft = atlasBounds.get("left").getAsFloat();
                  float atlasBottom = atlasBounds.get("bottom").getAsFloat();
                  float atlasRight = atlasBounds.get("right").getAsFloat();
                  float atlasTop = atlasBounds.get("top").getAsFloat();
                  float u0 = atlasLeft / atlasWidth;
                  float v0 = atlasBottom / atlasHeight;
                  float u1 = atlasRight / atlasWidth;
                  float v1 = atlasTop / atlasHeight;
                  float padding = 0.0F / atlasWidth;
                  u0 += padding;
                  u1 -= padding;
                  v0 += padding;
                  v1 -= padding;
                  float atlasU0 = borderU1 + u0 * (borderU2 - borderU1);
                  float atlasV0 = borderV1 + v0 * (borderV2 - borderV1);
                  float atlasU1 = borderU1 + u1 * (borderU2 - borderU1);
                  float atlasV1 = borderV1 + v1 * (borderV2 - borderV1);
                  glyph = new MsdfFont.Glyph(advance, planeLeft, planeBottom, planeRight, planeTop, atlasU0, 1.0F - atlasV1, atlasU1, 1.0F - atlasV0);
               } else {
                  glyph = new MsdfFont.Glyph(advance);
               }

               this.glyphs.put(codepoint, glyph);
            }
         }

         this.kerning = new HashMap<>();
         JsonArray kerningArray = root.getAsJsonArray("kerning");
         if (kerningArray != null) {
            for (JsonElement element : kerningArray) {
               JsonObject kObj = element.getAsJsonObject();
               int left = kObj.get("unicode1").getAsInt();
               int right = kObj.get("unicode2").getAsInt();
               float advance = kObj.has("advance") ? kObj.get("advance").getAsFloat() : 0.0F;
               this.kerning.put(pairKey(left, right), advance);
            }
         }
      } catch (Exception var43) {
         throw new RuntimeException("Failed to load MSDF font: " + jsonPath, var43);
      }
   }

   private String readJsonFile(String path) throws Exception {
      String var3;
      try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(path)) {
         if (is == null) {
            throw new RuntimeException("Resource not found: " + path);
         }

         var3 = new String(is.readAllBytes());
      }

      return var3;
   }

   private static long pairKey(int left, int right) {
      return (long)left << 32 | right & 4294967295L;
   }

   public MsdfFont.Glyph getGlyph(int codepoint) {
      return this.glyphs.get(codepoint);
   }

   public float getKerning(int left, int right) {
      return this.kerning.getOrDefault(pairKey(left, right), 0.0F);
   }

   public GLTexture getTexture() {
      return this.texture;
   }

   public TextureAtlas getAtlas() {
      return this.atlas;
   }

   public float getDistanceRange() {
      return this.distanceRange;
   }

   public float getEmSize() {
      return this.emSize;
   }

   public float getLineHeight() {
      return this.lineHeight;
   }

   public float getAscender() {
      return this.ascender;
   }

   public float getDescender() {
      return this.descender;
   }

   public static class Glyph {
      public final float advance;
      public final boolean renderable;
      public final float planeLeft;
      public final float planeBottom;
      public final float planeRight;
      public final float planeTop;
      public final float u0;
      public final float v0;
      public final float u1;
      public final float v1;

      public Glyph(float advance) {
         this.advance = advance;
         this.renderable = false;
         this.planeLeft = 0.0F;
         this.planeBottom = 0.0F;
         this.planeRight = 0.0F;
         this.planeTop = 0.0F;
         this.u0 = 0.0F;
         this.v0 = 0.0F;
         this.u1 = 0.0F;
         this.v1 = 0.0F;
      }

      public Glyph(float advance, float planeLeft, float planeBottom, float planeRight, float planeTop, float u0, float v0, float u1, float v1) {
         this.advance = advance;
         this.renderable = true;
         this.planeLeft = planeLeft;
         this.planeBottom = planeBottom;
         this.planeRight = planeRight;
         this.planeTop = planeTop;
         this.u0 = u0;
         this.v0 = v0;
         this.u1 = u1;
         this.v1 = v1;
      }
   }
}
