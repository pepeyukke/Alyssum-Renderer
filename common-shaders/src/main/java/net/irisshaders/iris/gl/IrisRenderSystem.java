package net.irisshaders.iris.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

import net.irisshaders.iris.gl.sampler.SamplerLimits;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

/**
 * This class is responsible for abstracting calls to OpenGL and asserting that calls are run on the render thread.
 */
public class IrisRenderSystem {
	private static final int[] emptyArray = new int[SamplerLimits.get().getMaxTextureUnits()];
	private static Matrix4f backupProjection;
	private static DSAAccess dsaState;
	private static boolean hasMultibind;
	private static boolean supportsCompute;
	private static boolean supportsTesselation;
	private static int polygonMode = GL43C.GL_FILL;
	private static int backupPolygonMode = GL43C.GL_FILL;
	private static int[] samplers;

	public static void initRenderer() {
		if (GL.getCapabilities().OpenGL45) {
			dsaState = new DSACore();
			IRIS_LOGGER.info("OpenGL 4.5 detected, enabling DSA.");
		} else if (GL.getCapabilities().GL_ARB_direct_state_access) {
			dsaState = new DSAARB();
			IRIS_LOGGER.info("ARB_direct_state_access detected, enabling DSA.");
		} else {
			dsaState = new DSAUnsupported();
			IRIS_LOGGER.info("DSA support not detected.");
		}

		hasMultibind = GL.getCapabilities().OpenGL45 || GL.getCapabilities().GL_ARB_multi_bind;

		supportsCompute = GL.getCapabilities().glDispatchCompute != MemoryUtil.NULL;
		supportsTesselation = GL.getCapabilities().GL_ARB_tessellation_shader || GL.getCapabilities().OpenGL40;

		samplers = new int[SamplerLimits.get().getMaxTextureUnits()];
	}

	public static void getIntegerv(int pname, int[] params) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glGetIntegerv(pname, params);
	}

	public static void getFloatv(int pname, float[] params) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glGetFloatv(pname, params);
	}

	public static void generateMipmaps(int texture, int mipmapTarget) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.generateMipmaps(texture, mipmapTarget);
	}

	public static void bindAttributeLocation(int program, int index, CharSequence name) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glBindAttribLocation(program, index, name);
	}

	public static void texImage1D(int texture, int target, int level, int internalformat, int width, int border, int format, int type, @Nullable ByteBuffer pixels) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		IrisRenderSystem.bindTextureForSetup(target, texture);
		GL30C.glTexImage1D(target, level, internalformat, width, border, format, type, pixels);
	}

	public static void texImage2D(int texture, int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		IrisRenderSystem.bindTextureForSetup(target, texture);
		GL32C.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	public static void texImage3D(int texture, int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, @Nullable ByteBuffer pixels) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		IrisRenderSystem.bindTextureForSetup(target, texture);
		GL30C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
	}

	public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer matrix) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniformMatrix4fv(location, transpose, matrix);
	}

	public static void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
	}

	public static void uniform1f(int location, float v0) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform1f(location, v0);
	}

	public static void uniform2f(int location, float v0, float v1) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform2f(location, v0, v1);
	}

	public static void uniform2i(int location, int v0, int v1) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform2i(location, v0, v1);
	}

	public static void uniform3f(int location, float v0, float v1, float v2) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform3f(location, v0, v1, v2);
	}

	public static void uniform3i(int location, int v0, int v1, int v2) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform3i(location, v0, v1, v2);
	}

	public static void uniform4f(int location, float v0, float v1, float v2, float v3) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform4f(location, v0, v1, v2, v3);
	}

	public static void uniform4i(int location, int v0, int v1, int v2, int v3) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniform4i(location, v0, v1, v2, v3);
	}

	public static void texParameteriv(int texture, int target, int pname, int[] params) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.texParameteriv(texture, target, pname, params);
	}

	/**
	 * Internal API for use when you don't know the target texture. Should use {@link IrisRenderSystem#texParameteriv(int, int, int, int[])} instead unless you know what you're doing!
	 */
	public static void texParameterivDirect(int target, int pname, int[] params) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glTexParameteriv(target, pname, params);
	}

	public static void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
		dsaState.copyTexSubImage2D(destTexture, target, i, i1, i2, i3, i4, width, height);
	}

	public static void texParameteri(int texture, int target, int pname, int param) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.texParameteri(texture, target, pname, param);
	}

	public static void texParameterf(int texture, int target, int pname, float param) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.texParameterf(texture, target, pname, param);
	}

	public static String getProgramInfoLog(int program) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return GL32C.glGetProgramInfoLog(program);
	}

	public static String getShaderInfoLog(int shader) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return GL32C.glGetShaderInfoLog(shader);
	}

	public static void drawBuffers(int framebuffer, int[] buffers) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.drawBuffers(framebuffer, buffers);
	}

	public static void readBuffer(int framebuffer, int buffer) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		dsaState.readBuffer(framebuffer, buffer);
	}

	public static String getActiveUniform(int program, int index, int size, IntBuffer type, IntBuffer name) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return GL32C.glGetActiveUniform(program, index, size, type, name);
	}

	public static void readPixels(int x, int y, int width, int height, int format, int type, float[] pixels) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glReadPixels(x, y, width, height, format, type, pixels);
	}

	public static void bufferData(int target, float[] data, int usage) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glBufferData(target, data, usage);
	}

	public static int bufferStorage(int target, float[] data, int usage) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return dsaState.bufferStorage(target, data, usage);
	}

	public static void bufferStorage(int target, long size, int flags) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		// The ARB version is identical to GL44 and redirects, so this should work on ARB as well.
		GL45C.glBufferStorage(target, size, flags);
	}

	public static void bindBufferBase(int target, Integer index, int buffer) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL43C.glBindBufferBase(target, index, buffer);
	}

	public static void vertexAttrib4f(int index, float v0, float v1, float v2, float v3) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glVertexAttrib4f(index, v0, v1, v2, v3);
	}

	public static void detachShader(int program, int shader) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glDetachShader(program, shader);
	}

	public static void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
		dsaState.framebufferTexture2D(fb, fbtarget, attachment, target, texture, levels);
	}

	public static int getTexParameteri(int texture, int target, int pname) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return dsaState.getTexParameteri(texture, target, pname);
	}

	public static void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		if (GL.getCapabilities().OpenGL42 || GL.getCapabilities().GL_ARB_shader_image_load_store) {
			GL42C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
		} else {
			EXTShaderImageLoadStore.glBindImageTextureEXT(unit, texture, level, layered, layer, access, format);
		}
	}

	public static int getMaxImageUnits() {
		if (GL.getCapabilities().OpenGL42 || GL.getCapabilities().GL_ARB_shader_image_load_store) {
			return GL_STATE_MANAGER.glGetInteger(GL42C.GL_MAX_IMAGE_UNITS);
		} else if (GL.getCapabilities().GL_EXT_shader_image_load_store) {
			return GL_STATE_MANAGER.glGetInteger(EXTShaderImageLoadStore.GL_MAX_IMAGE_UNITS_EXT);
		} else {
			return 0;
		}
	}

	public static boolean supportsSSBO() {
		return GL.getCapabilities().OpenGL44 || (GL.getCapabilities().GL_ARB_shader_storage_buffer_object && GL.getCapabilities().GL_ARB_buffer_storage);
	}

	public static boolean supportsImageLoadStore() {
		return GL.getCapabilities().glBindImageTexture != 0L || GL.getCapabilities().OpenGL42 || ((GL.getCapabilities().GL_ARB_shader_image_load_store || GL.getCapabilities().GL_EXT_shader_image_load_store) && GL.getCapabilities().GL_ARB_buffer_storage);
	}

	public static void genBuffers(int[] buffers) {
		GL43C.glGenBuffers(buffers);
	}

	public static void clearBufferSubData(int glShaderStorageBuffer, int glR8, long offset, long size, int glRed, int glByte, int[] ints) {
		GL43C.glClearBufferSubData(glShaderStorageBuffer, glR8, offset, size, glRed, glByte, ints);
	}

	public static void getProgramiv(int program, int value, int[] storage) {
		GL32C.glGetProgramiv(program, value, storage);
	}

	public static void dispatchCompute(int workX, int workY, int workZ) {
		GL45C.glDispatchCompute(workX, workY, workZ);
	}

	public static void dispatchCompute(Vector3i workGroups) {
		GL45C.glDispatchCompute(workGroups.x, workGroups.y, workGroups.z);
	}

	public static void memoryBarrier(int barriers) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();

		if (supportsCompute) {
			GL45C.glMemoryBarrier(barriers);
		}
	}

	public static boolean supportsBufferBlending() {
		return GL.getCapabilities().GL_ARB_draw_buffers_blend || GL.getCapabilities().OpenGL40;
	}

	public static void disableBufferBlend(int buffer) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glDisablei(GL32C.GL_BLEND, buffer);
        RENDER_SYSTEM.setUnknownBlendState();
	}

	public static void enableBufferBlend(int buffer) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glEnablei(GL32C.GL_BLEND, buffer);
        RENDER_SYSTEM.setUnknownBlendState();
	}

	public static void blendFuncSeparatei(int buffer, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		ARBDrawBuffersBlend.glBlendFuncSeparateiARB(buffer, srcRGB, dstRGB, srcAlpha, dstAlpha);
	}

	// These functions are deprecated and unavailable in the core profile.

	public static void bindTextureToUnit(int target, int unit, int texture) {
		dsaState.bindTextureToUnit(target, unit, texture);
	}

	public static int getUniformBlockIndex(int program, String uniformBlockName) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		return GL32C.glGetUniformBlockIndex(program, uniformBlockName);
	}

	public static void uniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL32C.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
	}

	public static void setShadowProjection(Matrix4f shadowProjection) {
		backupProjection = RENDER_SYSTEM.getProjectionMatrix();
        RENDER_SYSTEM.setProjectionMatrixOrth(shadowProjection);
	}

	public static void restorePlayerProjection() {
        RENDER_SYSTEM.setProjectionMatrixOrigin(backupProjection);
		backupProjection = null;
	}

	public static void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
		dsaState.blitFramebuffer(source, dest, offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
	}

	public static int createFramebuffer() {
		return dsaState.createFramebuffer();
	}

	public static int createTexture(int target) {
		return dsaState.createTexture(target);
	}

	public static void bindTextureForSetup(int glType, int glId) {
		GL30C.glBindTexture(glType, glId);
	}

	public static boolean supportsCompute() {
		return supportsCompute;
	}

	public static boolean supportsTesselation() {
		return supportsTesselation;
	}

	public static int genSampler() {
		return GL33C.glGenSamplers();
	}

	public static void destroySampler(int glId) {
		GL33C.glDeleteSamplers(glId);
	}

	public static void bindSamplerToUnit(int unit, int sampler) {
		if (samplers[unit] == sampler) {
			return;
		}

		GL33C.glBindSampler(unit, sampler);

		samplers[unit] = sampler;
	}

	public static void unbindAllSamplers() {
		boolean usedASampler = false;
		for (int i = 0; i < samplers.length; i++) {
			if (samplers[i] != 0) {
				usedASampler = true;
				if (!hasMultibind) GL33C.glBindSampler(i, 0);
				samplers[i] = 0;
			}
		}
		if (usedASampler && hasMultibind) {
			GL45C.glBindSamplers(0, emptyArray);
		}
	}


	public static void samplerParameteri(int sampler, int pname, int param) {
		GL33C.glSamplerParameteri(sampler, pname, param);
	}

	public static void samplerParameterf(int sampler, int pname, float param) {
		GL33C.glSamplerParameterf(sampler, pname, param);
	}

	public static void samplerParameteriv(int sampler, int pname, int[] params) {
		GL33C.glSamplerParameteriv(sampler, pname, params);
	}

	public static long getVRAM() {
		if (GL.getCapabilities().GL_NVX_gpu_memory_info) {
			return GL32C.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX) * 1024L;
		} else {
			return 4294967296L;
		}
	}

	public static void deleteBuffers(int glId) {
		RENDER_SYSTEM.assertOnRenderThreadOrInit();
		GL43C.glDeleteBuffers(glId);
	}

	public static void setPolygonMode(int mode) {
		if (mode != polygonMode) {
			polygonMode = mode;
			GL43C.glPolygonMode(GL43C.GL_FRONT_AND_BACK, mode);
		}
	}

	public static void overridePolygonMode() {
		backupPolygonMode = polygonMode;
		setPolygonMode(GL43C.GL_FILL);
	}

	public static void restorePolygonMode() {
		setPolygonMode(backupPolygonMode);
		backupPolygonMode = GL43C.GL_FILL;
	}

	public static void dispatchComputeIndirect(long offset) {
		GL43C.glDispatchComputeIndirect(offset);
	}

	public static void bindBuffer(int target, int buffer) {
		GL46C.glBindBuffer(target, buffer);
	}

	public static int createBuffers() {
		return dsaState.createBuffers();
	}

    private static boolean cullingState;

    public static void backupAndDisableCullingState(boolean b) {
        cullingState = MINECRAFT_SHIM.getSmartCull();
        MINECRAFT_SHIM.setSmartCull(cullingState && !b);
    }

    public static void restoreCullingState() {
        MINECRAFT_SHIM.setSmartCull(cullingState);
        cullingState = true;
    }

	public interface DSAAccess {
		void generateMipmaps(int texture, int target);

		void texParameteri(int texture, int target, int pname, int param);

		void texParameterf(int texture, int target, int pname, float param);

		void texParameteriv(int texture, int target, int pname, int[] params);

		void readBuffer(int framebuffer, int buffer);

		void drawBuffers(int framebuffer, int[] buffers);

		int getTexParameteri(int texture, int target, int pname);

		void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height);

		void bindTextureToUnit(int target, int unit, int texture);

		int bufferStorage(int target, float[] data, int usage);

		void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter);

		void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels);

		int createFramebuffer();

		int createTexture(int target);

		int createBuffers();
	}

	public static class DSACore extends DSAARB {

	}

	public static class DSAARB extends DSAUnsupported {

		@Override
		public void generateMipmaps(int texture, int target) {
			ARBDirectStateAccess.glGenerateTextureMipmap(texture);
		}

		@Override
		public void texParameteri(int texture, int target, int pname, int param) {
			ARBDirectStateAccess.glTextureParameteri(texture, pname, param);
		}

		@Override
		public void texParameterf(int texture, int target, int pname, float param) {
			ARBDirectStateAccess.glTextureParameterf(texture, pname, param);
		}

		@Override
		public void texParameteriv(int texture, int target, int pname, int[] params) {
			ARBDirectStateAccess.glTextureParameteriv(texture, pname, params);
		}

		@Override
		public void readBuffer(int framebuffer, int buffer) {
			ARBDirectStateAccess.glNamedFramebufferReadBuffer(framebuffer, buffer);
		}

		@Override
		public void drawBuffers(int framebuffer, int[] buffers) {
			ARBDirectStateAccess.glNamedFramebufferDrawBuffers(framebuffer, buffers);
		}

		@Override
		public int getTexParameteri(int texture, int target, int pname) {
			return ARBDirectStateAccess.glGetTextureParameteri(texture, pname);
		}

		@Override
		public void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
			ARBDirectStateAccess.glCopyTextureSubImage2D(destTexture, i, i1, i2, i3, i4, width, height);
		}

		@Override
		public void bindTextureToUnit(int target, int unit, int texture) {
			if (GL_STATE_MANAGER.getBoundTexture(unit) == texture) {
				return;
			}

			ARBDirectStateAccess.glBindTextureUnit(unit, texture);

			// Manually fix GLStateManager bindings...
            GL_STATE_MANAGER.setBoundTexture(unit, texture);
		}

		@Override
		public int bufferStorage(int target, float[] data, int usage) {
			int buffer = GL45C.glCreateBuffers();
			GL45C.glNamedBufferData(buffer, data, usage);
			return buffer;
		}

		@Override
		public int createBuffers() {
			return ARBDirectStateAccess.glCreateBuffers();
		}

		@Override
		public void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
			ARBDirectStateAccess.glBlitNamedFramebuffer(source, dest, offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
		}

		@Override
		public void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
			ARBDirectStateAccess.glNamedFramebufferTexture(fb, attachment, texture, levels);
		}

		@Override
		public int createFramebuffer() {
			return ARBDirectStateAccess.glCreateFramebuffers();
		}

		@Override
		public int createTexture(int target) {
			return ARBDirectStateAccess.glCreateTextures(target);
		}
	}

	public static class DSAUnsupported implements DSAAccess {
		@Override
		public void generateMipmaps(int texture, int target) {
			GL_STATE_MANAGER.bindTexture(texture);
			GL32C.glGenerateMipmap(target);
		}

		@Override
		public void texParameteri(int texture, int target, int pname, int param) {
			bindTextureForSetup(target, texture);
			GL32C.glTexParameteri(target, pname, param);
		}

		@Override
		public void texParameterf(int texture, int target, int pname, float param) {
			bindTextureForSetup(target, texture);
			GL32C.glTexParameterf(target, pname, param);
		}

		@Override
		public void texParameteriv(int texture, int target, int pname, int[] params) {
			bindTextureForSetup(target, texture);
			GL32C.glTexParameteriv(target, pname, params);
		}

		@Override
		public void readBuffer(int framebuffer, int buffer) {
			GL_STATE_MANAGER.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, framebuffer);
			GL32C.glReadBuffer(buffer);
		}

		@Override
		public void drawBuffers(int framebuffer, int[] buffers) {
			GL_STATE_MANAGER.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, framebuffer);
			GL32C.glDrawBuffers(buffers);
		}

		@Override
		public int getTexParameteri(int texture, int target, int pname) {
			bindTextureForSetup(target, texture);
			return GL32C.glGetTexParameteri(target, pname);
		}

		@Override
		public void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
			int previous = GL_STATE_MANAGER.getActiveBoundTexture();
			GL_STATE_MANAGER.bindTexture(destTexture);
			GL32C.glCopyTexSubImage2D(target, i, i1, i2, i3, i4, width, height);
			GL_STATE_MANAGER.bindTexture(previous);
		}

		@Override
		public void bindTextureToUnit(int target, int unit, int texture) {
			int activeTexture = GL_STATE_MANAGER.getActiveTexture();
			GL_STATE_MANAGER.glActiveTexture(GL30C.GL_TEXTURE0 + unit);
			bindTextureForSetup(target, texture);
			GL_STATE_MANAGER.glActiveTexture(activeTexture);
		}

		@Override
		public int bufferStorage(int target, float[] data, int usage) {
			int buffer = GL_STATE_MANAGER.glGenBuffers();
			GL_STATE_MANAGER.glBindBuffer(target, buffer);
			bufferData(target, data, usage);
			GL_STATE_MANAGER.glBindBuffer(target, 0);

			return buffer;
		}

		@Override
		public void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
			GL_STATE_MANAGER.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, source);
			GL_STATE_MANAGER.glBindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, dest);
			GL32C.glBlitFramebuffer(offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
		}

		@Override
		public void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
			GL_STATE_MANAGER.glBindFramebuffer(fbtarget, fb);
			GL32C.glFramebufferTexture2D(fbtarget, attachment, target, texture, levels);
		}

		@Override
		public int createFramebuffer() {
			int framebuffer = GL_STATE_MANAGER.glGenFramebuffers();
			GL_STATE_MANAGER.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, framebuffer);
			return framebuffer;
		}

		@Override
		public int createTexture(int target) {
			int texture = GL_STATE_MANAGER.glGenTextures();
			GL_STATE_MANAGER.bindTexture(texture);
			return texture;
		}

		@Override
		public int createBuffers() {
			int value = GL_STATE_MANAGER.glGenBuffers();
			return value;
		}
	}

	/*
	public static void bindTextures(int startingTexture, int[] bindings) {
		if (hasMultibind) {
			ARBMultiBind.glBindTextures(startingTexture, bindings);
		} else if (dsaState != DSAState.NONE) {
			for (int binding : bindings) {
				ARBDirectStateAccess.glBindTextureUnit(startingTexture, binding);
				startingTexture++;
			}
		} else {
			for (int binding : bindings) {
				GL_STATE_MANAGER.glActiveTexture(startingTexture);
				GL_STATE_MANAGER.bindTexture(binding);
				startingTexture++;
			}
		}
	}
	 */
}
