package org.embeddedt.embeddium.api.math;

//? if >=1.15 <1.20 {
/*import com.mojang.math.Matrix3f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Contract;

public interface Matrix3fExtended {
    /^*
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     ^/
    void rotate(Quaternion quaternion);

    int computeNormal(Direction dir);

    float transformVecX(float x, float y, float z);

    float transformVecY(float x, float y, float z);

    float transformVecZ(float x, float y, float z);

    default float transformVecX(Vector3f dir) {
        return this.transformVecX(dir.x(), dir.y(), dir.z());
    }

    default float transformVecY(Vector3f dir) {
        return this.transformVecY(dir.x(), dir.y(), dir.z());
    }

    default float transformVecZ(Vector3f dir) {
        return this.transformVecZ(dir.x(), dir.y(), dir.z());
    }

    float getA00();

    float getA10();

    float getA20();

    float getA01();

    float getA11();

    float getA21();

    float getA02();

    float getA12();

    float getA22();

    int transformNormal(Direction direction);

    @Contract(pure = true)
    static Matrix3fExtended get(Matrix3f matrix) {
        return (Matrix3fExtended)(Object)matrix;
    }
}
*///?} else {
public interface Matrix3fExtended {}
//?}

