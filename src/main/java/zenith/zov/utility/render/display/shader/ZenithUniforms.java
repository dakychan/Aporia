package zenith.zov.utility.render.display.shader;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;

final class ZenithUniforms {
    static final int BUFFER_SIZE = calculateSize();

    float sizeX;
    float sizeY;
    float radiusX;
    float radiusY;
    float radiusZ;
    float radiusW;
    float smoothnessX;
    float smoothnessY;
    float cornerSmoothness;
    float thickness;
    float cornerIndex;
    float progress;
    float fade;
    float stripeWidth;
    float topLeftR;
    float topLeftG;
    float topLeftB;
    float topLeftA;
    float bottomLeftR;
    float bottomLeftG;
    float bottomLeftB;
    float bottomLeftA;
    float topRightR;
    float topRightG;
    float topRightB;
    float topRightA;
    float bottomRightR;
    float bottomRightG;
    float bottomRightB;
    float bottomRightA;
    float blurRadius;
    float resolutionX;
    float resolutionY;
    float offset;
    float saturation = 1.0f;
    float tintIntensity;
    float tintColorR = 1.0f;
    float tintColorG = 1.0f;
    float tintColorB = 1.0f;
    float range;
    float outline;
    float outlineThickness;
    float outlineColorR;
    float outlineColorG;
    float outlineColorB;
    float outlineColorA;
    float enableFadeout;
    float fadeoutStart;
    float fadeoutEnd;
    float maxWidth;
    float textPosX;

    void write(Std140Builder builder) {
        builder
                .putVec2(sizeX, sizeY)
                .putVec4(radiusX, radiusY, radiusZ, radiusW)
                .putVec2(smoothnessX, smoothnessY)
                .putFloat(cornerSmoothness)
                .putFloat(thickness)
                .putFloat(cornerIndex)
                .putFloat(progress)
                .putFloat(fade)
                .putFloat(stripeWidth)
                .putVec4(topLeftR, topLeftG, topLeftB, topLeftA)
                .putVec4(bottomLeftR, bottomLeftG, bottomLeftB, bottomLeftA)
                .putVec4(topRightR, topRightG, topRightB, topRightA)
                .putVec4(bottomRightR, bottomRightG, bottomRightB, bottomRightA)
                .putFloat(blurRadius)
                .putVec2(resolutionX, resolutionY)
                .putFloat(offset)
                .putFloat(saturation)
                .putFloat(tintIntensity)
                .putVec3(tintColorR, tintColorG, tintColorB)
                .putFloat(range)
                .putFloat(outline)
                .putFloat(outlineThickness)
                .putVec4(outlineColorR, outlineColorG, outlineColorB, outlineColorA)
                .putFloat(enableFadeout)
                .putFloat(fadeoutStart)
                .putFloat(fadeoutEnd)
                .putFloat(maxWidth)
                .putFloat(textPosX);
    }

    private static int calculateSize() {
        return new Std140SizeCalculator()
                .putVec2()
                .putVec4()
                .putVec2()
                .putFloat()
                .putFloat()
                .putFloat()
                .putFloat()
                .putFloat()
                .putFloat()
                .putVec4()
                .putVec4()
                .putVec4()
                .putVec4()
                .putFloat()
                .putVec2()
                .putFloat()
                .putFloat()
                .putFloat()
                .putVec3()
                .putFloat()
                .putFloat()
                .putFloat()
                .putVec4()
                .putFloat()
                .putFloat()
                .putFloat()
                .putFloat()
                .putFloat()
                .get();
    }
}

