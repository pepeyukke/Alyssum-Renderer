package net.irisshaders.iris.gl.uniform;

import java.nio.FloatBuffer;
import java.util.function.Supplier;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import net.irisshaders.iris.gl.state.ValueUpdateNotifier;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class MatrixUniform extends Uniform {
	private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
	private final Supplier<Matrix4f> value;
	private Matrix4f cachedValue;

	MatrixUniform(int location, Supplier<Matrix4f> value) {
		super(location);

		this.cachedValue = null;
		this.value = value;
	}

	MatrixUniform(int location, Supplier<Matrix4f> value, ValueUpdateNotifier notifier) {
		super(location, notifier);

		this.cachedValue = null;
		this.value = value;
	}

	@Override
	public void update() {
		updateValue();

		if (notifier != null) {
			notifier.setListener(this::updateValue);
		}
	}

	public void updateValue() {
		Matrix4f newValue = value.get();

		if (!newValue.equals(cachedValue)) {
			cachedValue = new Matrix4f(newValue);

			cachedValue.get(buffer);
			buffer.rewind();

			RENDER_SYSTEM.glUniformMatrix4(location, false, buffer);
		}
	}
}
