package net.irisshaders.iris.mixin.vertices.immediate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Uses a priority of 999 to apply before the main Iris mixins to draw entities before deferred runs.
@Mixin(value = LevelRenderer.class, priority = 999)
public class MixinLevelRenderer {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$immediateStateBeginLevelRender(CallbackInfo ci) {
		ImmediateState.isRenderingLevel = true;
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void iris$immediateStateEndLevelRender(CallbackInfo ci) {
		ImmediateState.isRenderingLevel = false;
	}
}
