package zenith.zov.utility.render.display;

import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;

public final class MatrixUtil {
    private MatrixUtil() {
    }

    public static Matrix4f toMatrix4f(Matrix3x2fc matrix) {
        Matrix4f out = new Matrix4f();
        out.m00(matrix.m00());
        out.m01(matrix.m01());
        out.m10(matrix.m10());
        out.m11(matrix.m11());
        out.m30(matrix.m20());
        out.m31(matrix.m21());
        out.m22(1.0f);
        out.m33(1.0f);
        return out;
    }
}
