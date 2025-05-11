package net.irisshaders.iris.targets;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL43C;

public class DepthTexture extends GlResource {
	public DepthTexture(String name, int width, int height, DepthBufferFormat format) {
		super(IrisRenderSystem.createTexture(GL11C.GL_TEXTURE_2D));
		int texture = getGlId();

		resize(width, height, format);
		GLDebug.nameObject(GL43C.GL_TEXTURE, texture, name);

		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_EDGE);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_EDGE);

		GL_STATE_MANAGER.bindTexture(0);
	}

	void resize(int width, int height, DepthBufferFormat format) {
		IrisRenderSystem.texImage2D(getTextureId(), GL11C.GL_TEXTURE_2D, 0, format.getGlInternalFormat(), width, height, 0,
			format.getGlType(), format.getGlFormat(), null);
	}

	public int getTextureId() {
		return getGlId();
	}

	@Override
	protected void destroyInternal() {
		GL_STATE_MANAGER.glDeleteTextures(getGlId());
	}
}
