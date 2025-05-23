package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.ModernBiomeUniforms;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.irisshaders.iris.uniforms.BiomeUniforms.BIOME_UNIFORMS;

@Mixin(Biomes.class)
public class MixinBiomes {
	private static int currentId = 0;

	@Inject(method = "register", at = @At("TAIL"))
	private static void iris$registerBiome(String string, CallbackInfoReturnable<ResourceKey<Biome>> cir) {
        ((ModernBiomeUniforms)BIOME_UNIFORMS).getBiomeMap().put(cir.getReturnValue(), currentId++);
	}
}
