package org.taumc.celeritas.mixin.core.biome_blending;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockLeaves;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.taumc.celeritas.impl.extensions.WorldClientExtension;
import org.taumc.celeritas.impl.world.biome.SmoothBiomeColorCache;

@Mixin(BlockLeaves.class)
public class BlockLeavesMixin {
    @WrapMethod(method = "colorMultiplier")
    private int smoothBlendColor(IBlockAccess access, int x, int y, int z, Operation<Integer> original) {
        if (SmoothBiomeColorCache.enabled && access instanceof WorldClientExtension ext) {
            return ext.celeritas$getSmoothBiomeColorCache().getColor(SmoothBiomeColorCache.ColorType.FOLIAGE, x, y, z);
        } else {
            return original.call(access, x, y, z);
        }
    }
}
