package org.embeddedt.embeddium.impl.model.light.flat;

//? if forgelike && <1.19
/*import net.minecraftforge.client.model.pipeline.LightUtil;*/
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.api.util.NormI8;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.util.WorldUtil;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.model.light.data.LightDataAccess.*;

/**
 * A light pipeline which implements "classic-style" lighting through simply using the light value of the adjacent
 * block to a face.
 */
public class FlatLightPipeline implements LightPipeline {
    /**
     * The cache which light data will be accessed from.
     */
    private final LightDataAccess lightCache;

    /**
     * Whether or not to even attempt to shade quads using their normals rather than light face.
     */
    private final boolean useQuadNormalsForShading;

    /**
     * Whether directional shading should be enabled.
     */
    private final boolean enableDirectionalShading;

    public FlatLightPipeline(LightDataAccess lightCache, boolean enableDirectionalShading) {
        this.lightCache = lightCache;
        this.enableDirectionalShading = enableDirectionalShading;
        this.useQuadNormalsForShading = Celeritas.options().quality.useQuadNormalsForShading;
    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        int lightmap;

        // To match vanilla behavior, use the cull face if it exists/is available
        if (cullFace != null) {
            lightmap = getOffsetLightmap(pos, cullFace);
        } else {
            int flags = quad.getFlags();
            // If the face is aligned, use the light data above it
            // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(pos)))) {
                lightmap = getOffsetLightmap(pos, lightFace);
            } else {
                lightmap = getEmissiveLightmap(this.lightCache.get(pos));
            }
        }

        Arrays.fill(out.lm, lightmap);

        if (enableDirectionalShading) {
            //? if forgelike
            if ((quad.getFlags() & ModelQuadFlags.IS_VANILLA_SHADED) != 0 || !this.useQuadNormalsForShading) {
                Arrays.fill(out.br, WorldUtil.getShade(this.lightCache.getWorld(), lightFace, shade));
            /*? if forgelike {*/ } else {
                this.applySidedBrightnessFromNormals(quad, out, shade);
            } /*?}*/
        } else {
            Arrays.fill(out.br, 1.0f);
        }
    }

    //? if forgelike && >=1.19 {
    public void applySidedBrightnessFromNormals(ModelQuadView quad, QuadLightData out, boolean shade) {
        int normal = quad.getModFaceNormal();
        Arrays.fill(out.br, this.lightCache.getWorld().getShade(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), shade));
    }
    //?} else if forgelike && <=1.19 {
    /*private void applySidedBrightnessFromNormals(ModelQuadView quad, QuadLightData out, boolean shade) {
        int normal = quad.getModFaceNormal();
        Arrays.fill(out.br, shade ? LightUtil.diffuseLight(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal)) : 1.0f);
    }
    *///?}

    /**
     * When vanilla computes an offset lightmap with flat lighting, it passes the original BlockState but the
     * offset BlockPos to {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)}.
     * This does not make much sense but fixes certain issues, primarily dark quads on light-emitting blocks
     * behind tinted glass. {@link LightDataAccess} cannot efficiently store lightmaps computed with
     * inconsistent values so this method exists to mirror vanilla behavior as closely as possible.
     */
    private int getOffsetLightmap(BlockPos pos, Direction face) {
        int word = this.lightCache.get(pos);

        // Check emissivity of the origin state
        if (unpackEM(word)) {
            return LightDataAccess.FULL_BRIGHT;
        }

        // Use world light values from the offset pos, but luminance from the origin pos
        int adjWord = this.lightCache.get(pos, face);
        return LightTexture.pack(Math.max(unpackBL(adjWord), unpackLU(word)), unpackSL(adjWord));
    }
}
