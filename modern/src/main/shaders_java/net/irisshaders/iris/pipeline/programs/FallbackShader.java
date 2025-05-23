package net.irisshaders.iris.pipeline.programs;

import java.io.IOException;
import java.util.List;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.ModernIrisRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;

public class FallbackShader extends ShaderInstance {
	private final ModernIrisRenderingPipeline parent;
	private final BlendModeOverride blendModeOverride;
	private final GlFramebuffer writingToBeforeTranslucent;
	private final GlFramebuffer writingToAfterTranslucent;

	@Nullable
	private final Uniform FOG_DENSITY;

	@Nullable
	private final Uniform FOG_IS_EXP2;
	private final int gtexture;
	private final int overlay;
	private final int lightmap;

	public FallbackShader(ResourceProvider resourceFactory, String string, VertexFormat vertexFormat,
						  GlFramebuffer writingToBeforeTranslucent, GlFramebuffer writingToAfterTranslucent,
						  BlendModeOverride blendModeOverride, float alphaValue, ModernIrisRenderingPipeline parent) throws IOException {
		super(resourceFactory, string, vertexFormat);

		this.parent = parent;
		this.blendModeOverride = blendModeOverride;
		this.writingToBeforeTranslucent = writingToBeforeTranslucent;
		this.writingToAfterTranslucent = writingToAfterTranslucent;

		this.FOG_DENSITY = this.getUniform("FogDensity");
		this.FOG_IS_EXP2 = this.getUniform("FogIsExp2");

		this.gtexture = GL_STATE_MANAGER.glGetUniformLocation(getId(), "gtexture");
		this.overlay = GL_STATE_MANAGER.glGetUniformLocation(getId(), "overlay");
		this.lightmap = GL_STATE_MANAGER.glGetUniformLocation(getId(), "lightmap");


		Uniform ALPHA_TEST_VALUE = this.getUniform("AlphaTestValue");

		if (ALPHA_TEST_VALUE != null) {
			ALPHA_TEST_VALUE.set(alphaValue);
		}
	}

	@Override
	public void clear() {
		super.clear();

		if (this.blendModeOverride != null) {
			BlendModeOverride.restore();
		}

		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}

	@Override
	public void apply() {
		if (FOG_DENSITY != null && FOG_IS_EXP2 != null) {
			float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();

			if (fogDensity >= 0.0) {
				FOG_DENSITY.set(fogDensity);
				FOG_IS_EXP2.set(1);
			} else {
				FOG_DENSITY.set(0.0F);
				FOG_IS_EXP2.set(0);
			}
		}

		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.ALBEDO_TEXTURE_UNIT, RENDER_SYSTEM.getShaderTexture(0));
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.OVERLAY_TEXTURE_UNIT, RENDER_SYSTEM.getShaderTexture(1));
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.LIGHTMAP_TEXTURE_UNIT, RENDER_SYSTEM.getShaderTexture(2));

		GL_STATE_MANAGER.glUseProgram(this.getId());

		List<Uniform> uniformList = super.uniforms;
		for (Uniform uniform : uniformList) {
			uploadIfNotNull(uniform);
		}

		GL_STATE_MANAGER.glUniform1i(gtexture, 0);
		GL_STATE_MANAGER.glUniform1i(overlay, 1);
		GL_STATE_MANAGER.glUniform1i(lightmap, 2);

		if (this.blendModeOverride != null) {
			this.blendModeOverride.apply();
		}

		if (parent.isBeforeTranslucent) {
			writingToBeforeTranslucent.bind();
		} else {
			writingToAfterTranslucent.bind();
		}
	}

	private void uploadIfNotNull(Uniform uniform) {
		if (uniform != null) {
			uniform.upload();
		}
	}
}
