package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TheEndPortalRenderer.class)
public class MixinTheEndPortalRenderer {
	@Unique
	private static final float RED = 0.075f;

	@Unique
	private static final float GREEN = 0.15f;

	@Unique
	private static final float BLUE = 0.2f;

	@Shadow
	protected float getOffsetUp() {
		return 0.75F;
	}

	@Shadow
	protected float getOffsetDown() {
		return 0.375F;
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	public void iris$onRender(TheEndPortalBlockEntity entity, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, CallbackInfo ci) {
		if (!IrisCommon.getCurrentPack().isPresent()) {
			return;
		}

		ci.cancel();

		// POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
		VertexConsumer vertexConsumer =
			multiBufferSource.getBuffer(RenderType.entitySolid(TheEndPortalRenderer.END_PORTAL_LOCATION));

        PoseStack.Pose pose = poseStack.last();
		Matrix3f normal = poseStack.last().normal();

		// animation with a period of 100 seconds.
		// note that texture coordinates are wrapping, not clamping.
		float progress = (SystemTimeUniforms.TIMER.getFrameTimeCounter() * 0.01f) % 1f;
		float topHeight = getOffsetUp();
		float bottomHeight = getOffsetDown();

		quad(entity, vertexConsumer, pose, normal, Direction.UP, progress, overlay, light,
			0.0f, topHeight, 1.0f,
			1.0f, topHeight, 1.0f,
			1.0f, topHeight, 0.0f,
			0.0f, topHeight, 0.0f);

		quad(entity, vertexConsumer, pose, normal, Direction.DOWN, progress, overlay, light,
			0.0f, bottomHeight, 1.0f,
			0.0f, bottomHeight, 0.0f,
			1.0f, bottomHeight, 0.0f,
			1.0f, bottomHeight, 1.0f);

		quad(entity, vertexConsumer, pose, normal, Direction.NORTH, progress, overlay, light,
			0.0f, topHeight, 0.0f,
			1.0f, topHeight, 0.0f,
			1.0f, bottomHeight, 0.0f,
			0.0f, bottomHeight, 0.0f);

		quad(entity, vertexConsumer, pose, normal, Direction.WEST, progress, overlay, light,
			0.0f, topHeight, 1.0f,
			0.0f, topHeight, 0.0f,
			0.0f, bottomHeight, 0.0f,
			0.0f, bottomHeight, 1.0f);

		quad(entity, vertexConsumer, pose, normal, Direction.SOUTH, progress, overlay, light,
			0.0f, topHeight, 1.0f,
			0.0f, bottomHeight, 1.0f,
			1.0f, bottomHeight, 1.0f,
			1.0f, topHeight, 1.0f);

		quad(entity, vertexConsumer, pose, normal, Direction.EAST, progress, overlay, light,
			1.0f, topHeight, 1.0f,
			1.0f, bottomHeight, 1.0f,
			1.0f, bottomHeight, 0.0f,
			1.0f, topHeight, 0.0f);
	}

	@Unique
	private void quad(TheEndPortalBlockEntity entity, VertexConsumer vertexConsumer,
                      //? if >=1.20.6 {
                      /*PoseStack.Pose pose, Matrix3f ignore,
                      *///?} else
                      PoseStack.Pose entry, Matrix3f normal,
					  Direction direction, float progress, int overlay, int light,
					  float x1, float y1, float z1,
					  float x2, float y2, float z2,
					  float x3, float y3, float z3,
					  float x4, float y4, float z4) {
		if (!entity.shouldRenderFace(direction)) {
			return;
		}

		float nx = direction.getStepX();
		float ny = direction.getStepY();
		float nz = direction.getStepZ();

        //? if <1.20.6
        var pose = entry.pose();
        //? if >=1.20.6
        /*var normal = pose;*/

        //? if <1.21 {
		vertexConsumer.vertex(pose, x1, y1, z1).color(RED, GREEN, BLUE, 1.0f)
			.uv(0.0F + progress, 0.0F + progress).overlayCoords(overlay).uv2(light)
			.normal(normal, nx, ny, nz).endVertex();

		vertexConsumer.vertex(pose, x2, y2, z2).color(RED, GREEN, BLUE, 1.0f)
			.uv(0.0F + progress, 0.2F + progress).overlayCoords(overlay).uv2(light)
			.normal(normal, nx, ny, nz).endVertex();

		vertexConsumer.vertex(pose, x3, y3, z3).color(RED, GREEN, BLUE, 1.0f)
			.uv(0.2F + progress, 0.2F + progress).overlayCoords(overlay).uv2(light)
			.normal(normal, nx, ny, nz).endVertex();

		vertexConsumer.vertex(pose, x4, y4, z4).color(RED, GREEN, BLUE, 1.0f)
			.uv(0.2F + progress, 0.0F + progress).overlayCoords(overlay).uv2(light)
			.normal(normal, nx, ny, nz).endVertex();
        //?} else {
        /*vertexConsumer.addVertex(pose, x1, y1, z1).setColor(RED, GREEN, BLUE, 1.0f)
                .setUv(0.0F + progress, 0.0F + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        vertexConsumer.addVertex(pose, x2, y2, z2).setColor(RED, GREEN, BLUE, 1.0f)
                .setUv(0.0F + progress, 0.2F + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        vertexConsumer.addVertex(pose, x3, y3, z3).setColor(RED, GREEN, BLUE, 1.0f)
                .setUv(0.2F + progress, 0.2F + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);

        vertexConsumer.addVertex(pose, x4, y4, z4).setColor(RED, GREEN, BLUE, 1.0f)
                .setUv(0.2F + progress, 0.0F + progress).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
        *///?}
	}
}
