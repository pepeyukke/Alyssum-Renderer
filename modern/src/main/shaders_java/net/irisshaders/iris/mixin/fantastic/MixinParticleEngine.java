package net.irisshaders.iris.mixin.fantastic;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.mixin.InjectionPoints;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.function.Supplier;

/**
 * Extends the ParticleEngine class to allow multiple phases of particle rendering.
 * <p>
 * This is used to enable the rendering of known-opaque particles much earlier than other particles, most notably before
 * translucent content. Normally, particles behind translucent blocks are not visible on Fancy graphics, and a user must
 * enable the much more intensive Fabulous graphics option. This is not ideal because Fabulous graphics is fundamentally
 * incompatible with most shader packs.
 * <p>
 * So what causes this? Essentially, on Fancy graphics, all particles are rendered after translucent terrain. Aside from
 * causing problems with particles being invisible, this also causes particles to write to the translucent depth buffer,
 * even when they are not translucent. This notably causes problems with particles on Sildur's Enhanced Default when
 * underwater.
 * <p>
 * So, what these mixins do is try to render known-opaque particles right before entities are rendered and right after
 * opaque terrain has been rendered. This seems to be an acceptable injection point, and has worked in my testing. It
 * fixes issues with particles when underwater, fixes a vanilla bug, and doesn't have any significant performance hit.
 * A win-win!
 * <p>
 * Unfortunately, there are limitations. Some particles rendering in texture sheets where translucency is supported. So,
 * even if an individual particle from that sheet is not translucent, it will still be treated as translucent, and thus
 * will not be affected by this patch. Without making more invasive and sweeping changes, there isn't a great way to get
 * around this.
 * <p>
 * As the saying goes, "Work smarter, not harder."
 */
@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements PhasedParticleEngine {
	private static final Set<ParticleRenderType> OPAQUE_PARTICLE_RENDER_TYPES;

	static {
		OPAQUE_PARTICLE_RENDER_TYPES = ImmutableSet.of(
				ParticleRenderType.PARTICLE_SHEET_OPAQUE,
				ParticleRenderType.PARTICLE_SHEET_LIT,
				ParticleRenderType.CUSTOM,
				ParticleRenderType.NO_RENDER
		);
	}

	@Unique
	private ParticleRenderingPhase phase = ParticleRenderingPhase.EVERYTHING;

	@Redirect(method = InjectionPoints.PARTICLE_ENGINE_RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"))
	private void iris$changeParticleShader(Supplier<ShaderInstance> pSupplier0) {
		RenderSystem.setShader(phase == ParticleRenderingPhase.TRANSLUCENT ? ShaderAccess::getParticleTranslucentShader : pSupplier0);
	}

	@ModifyExpressionValue(method = InjectionPoints.PARTICLE_ENGINE_RENDER, at = @At(value = "INVOKE", target = /*? if <1.21 {*/ "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;" /*?} else {*/ /*"Ljava/util/Queue;iterator()Ljava/util/Iterator;" *//*?}*/))
	private Iterator<Particle> iris$selectParticlesToRender(Iterator<Particle> iterator, @Local(ordinal = 0) ParticleRenderType renderType) {
        if (phase == ParticleRenderingPhase.NOTHING) {
            // Remove everything
            return Collections.emptyIterator();
        } else if (phase == ParticleRenderingPhase.TRANSLUCENT) {
			// Render only translucent particle sheets
			return !OPAQUE_PARTICLE_RENDER_TYPES.contains(renderType) ? iterator : Collections.emptyIterator();
		} else if (phase == ParticleRenderingPhase.OPAQUE) {
			// Render only opaque particle sheets
            return OPAQUE_PARTICLE_RENDER_TYPES.contains(renderType) ? iterator : Collections.emptyIterator();
		} else {
			// Don't override particle rendering
			return iterator;
		}
	}

	@Override
	public void setParticleRenderingPhase(ParticleRenderingPhase phase) {
		this.phase = phase;
	}
}
