// This file is based on code from Sodium by JellySquid, licensed under the LGPLv3 license.

package net.irisshaders.iris.gl.shader;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.KHRDebug;

import java.util.Locale;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlResource {
	private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

	private final String name;

	public GlShader(ShaderType type, String name, String src) {
		super(createShader(type, name, src));

		this.name = name;
	}

	private static int createShader(ShaderType type, String name, String src) {
		int handle = GL_STATE_MANAGER.glCreateShader(type.id);
		ShaderWorkarounds.safeShaderSource(handle, src);
		GL_STATE_MANAGER.glCompileShader(handle);

		GLDebug.nameObject(KHRDebug.GL_SHADER, handle, name + "(" + type.name().toLowerCase(Locale.ROOT) + ")");

		String log = IrisRenderSystem.getShaderInfoLog(handle);

		if (!log.isEmpty()) {
			LOGGER.warn("Shader compilation log for " + name + ": " + log);
		}

		int result = GL_STATE_MANAGER.glGetShaderi(handle, GL20C.GL_COMPILE_STATUS);

		if (result != GL20C.GL_TRUE) {
			throw new ShaderCompileException(name, log);
		}

		return handle;
	}

	public String getName() {
		return this.name;
	}

	public int getHandle() {
		return this.getGlId();
	}

	@Override
	protected void destroyInternal() {
		GL_STATE_MANAGER.glDeleteShader(this.getGlId());
	}
}
