package org.embeddedt.embeddium.impl.mixin.features.render.entity.fast_render;

import org.embeddedt.embeddium.impl.model.ModelCuboidAccessor;
import org.embeddedt.embeddium.impl.render.immediate.model.ModelCuboid;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumSet;
import java.util.Set;

@Mixin(ModelPart.Cube.class)
public class CuboidMixin implements ModelCuboidAccessor {

    @Mutable
    @Shadow
    @Final
    private float minX;

    @Unique
    private ModelCuboid sodium$cuboid;

    @Unique
    private ModelCuboid embeddium$simpleCuboid;

    // Inject at the start of the function, so we don't capture modified locals
    @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/model/geom/ModelPart$Cube;minX:F", ordinal = 0))
    private void onInit(ModelPart.Cube instance, float value, int u, int v, float x, float y, float z, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight/*? if >=1.20 {*/, Set<Direction> renderDirections/*?}*/) {
        //? if <1.20
        /*Set<Direction> renderDirections = EnumSet.allOf(Direction.class);*/
        this.sodium$cuboid = new ModelCuboid(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, renderDirections);
        this.embeddium$simpleCuboid = (Class<?>)getClass() == ModelPart.Cube.class ? this.sodium$cuboid : null;

        this.minX = value;
    }

    @Override
    public ModelCuboid sodium$copy() {
        return this.sodium$cuboid;
    }

    @Override
    public @Nullable ModelCuboid embeddium$getSimpleCuboid() {
        return this.embeddium$simpleCuboid;
    }
}
