package net.irisshaders.iris.texture;

import java.nio.IntBuffer;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;

public class TextureInfoCache {
	public static final TextureInfoCache INSTANCE = new TextureInfoCache();

	private final Int2ObjectMap<TextureInfo> cache = new Int2ObjectOpenHashMap<>();

	private TextureInfoCache() {
	}

	public TextureInfo getInfo(int id) {
		TextureInfo info = cache.get(id);
		if (info == null) {
			info = new TextureInfo(id);
			cache.put(id, info);
		}
		return info;
	}

	public void onTexImage2D(int target, int level, int internalformat, int width, int height, int border,
							 int format, int type, @Nullable IntBuffer pixels) {
		if (level == 0) {
			int id = GL_STATE_MANAGER.getActiveBoundTexture();
			TextureInfo info = getInfo(id);
			info.internalFormat = internalformat;
			info.width = width;
			info.height = height;
		}
	}

	public void onDeleteTexture(int id) {
		cache.remove(id);
	}

	public static class TextureInfo {
		private final int id;
		private int internalFormat = -1;
		private int width = -1;
		private int height = -1;

		private TextureInfo(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public int getInternalFormat() {
			if (internalFormat == -1) {
				internalFormat = fetchLevelParameter(GL20C.GL_TEXTURE_INTERNAL_FORMAT);
			}
			return internalFormat;
		}

		public int getWidth() {
			if (width == -1) {
				width = fetchLevelParameter(GL20C.GL_TEXTURE_WIDTH);
			}
			return width;
		}

		public int getHeight() {
			if (height == -1) {
				height = fetchLevelParameter(GL20C.GL_TEXTURE_HEIGHT);
			}
			return height;
		}

		private int fetchLevelParameter(int pname) {
			// Keep track of what texture was bound before
			int previousTextureBinding = GL_STATE_MANAGER.glGetInteger(GL20C.GL_TEXTURE_BINDING_2D);

			// Bind this texture and grab the parameter from it.
			GL_STATE_MANAGER.bindTexture(id);
			int parameter = GL_STATE_MANAGER.glGetTexLevelParameteri(GL20C.GL_TEXTURE_2D, 0, pname);

			// Make sure to re-bind the previous texture to avoid issues.
			GL_STATE_MANAGER.bindTexture(previousTextureBinding);

			return parameter;
		}
	}
}
