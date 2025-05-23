package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.ONCE;
import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

/**
 * @see <a href="https://github.com/IrisShaders/ShaderDoc/blob/master/uniforms.md#camera">Uniforms: Camera</a>
 */
public class CameraUniforms {

	private CameraUniforms() {
	}

	public static void addCameraUniforms(UniformHolder uniforms, FrameUpdateNotifier notifier) {
		CameraPositionTracker tracker = new CameraPositionTracker(notifier);

		uniforms
			.uniform1f(ONCE, "near", () -> 0.05)
			.uniform1f(PER_FRAME, "far", CameraUniforms::getRenderDistanceInBlocks)
			.uniform3d(PER_FRAME, "cameraPosition", tracker::getCurrentCameraPosition)
			.uniform1f(PER_FRAME, "eyeAltitude", tracker::getCurrentCameraPositionY)
			.uniform3d(PER_FRAME, "previousCameraPosition", tracker::getPreviousCameraPosition)
			.uniform3i(PER_FRAME, "cameraPositionInt", () -> getCameraPositionInt(getUnshiftedCameraPosition()))
			.uniform3f(PER_FRAME, "cameraPositionFract", () -> getCameraPositionFract(getUnshiftedCameraPosition()))
			.uniform3i(PER_FRAME, "previousCameraPositionInt", () -> getCameraPositionInt(tracker.getPreviousCameraPositionUnshifted()))
			.uniform3f(PER_FRAME, "previousCameraPositionFract", () -> getCameraPositionFract(tracker.getPreviousCameraPositionUnshifted()));
	}

	private static int getRenderDistanceInBlocks() {
		return MINECRAFT_SHIM.getRenderDistanceInBlocks();
	}

	public static Vector3d getUnshiftedCameraPosition() {
		return MINECRAFT_SHIM.getUnshiftedCameraPosition();
	}

	public static Vector3f getCameraPositionFract(Vector3d originalPos) {
		return new Vector3f(
			(float) (originalPos.x - Math.floor(originalPos.x)),
			(float) (originalPos.y - Math.floor(originalPos.y)),
			(float) (originalPos.z - Math.floor(originalPos.z))
		);
	}

	public static Vector3i getCameraPositionInt(Vector3d originalPos) {
		return new Vector3i(
			(int) Math.floor(originalPos.x),
			(int) Math.floor(originalPos.y),
			(int) Math.floor(originalPos.z)
		);
	}

	static class CameraPositionTracker {
		/**
		 * Value range of cameraPosition. We want this to be small enough that precision is maintained when we convert
		 * from a double to a float, but big enough that shifts happen infrequently, since each shift corresponds with
		 * a noticeable change in shader animations and similar. 1000024 is the number used by Optifine for walking (however this is too much, so we choose 30000),
		 * with an extra 1024 check for if the user has teleported between camera positions.
		 */
		private static final double WALK_RANGE = 30000;
		private static final double TP_RANGE = 1000;
		private final Vector3d shift = new Vector3d();
		private Vector3d previousCameraPosition = new Vector3d();
		private Vector3d currentCameraPosition = new Vector3d();
		private Vector3d previousCameraPositionUnshifted = new Vector3d();
		private Vector3d currentCameraPositionUnshifted = new Vector3d();

		CameraPositionTracker(FrameUpdateNotifier notifier) {
			notifier.addListener(this::update);
		}

		private static double getShift(double value, double prevValue) {
			if (Math.abs(value) > WALK_RANGE || Math.abs(value - prevValue) > TP_RANGE) {
				// Only shift by increments of WALK_RANGE - this is required for some packs (like SEUS PTGI) to work properly
				return -(value - (value % WALK_RANGE));
			} else {
				return 0.0;
			}
		}

		private void update() {
			previousCameraPosition = currentCameraPosition;
			previousCameraPositionUnshifted = currentCameraPositionUnshifted;
			currentCameraPosition = getUnshiftedCameraPosition().add(shift);
			currentCameraPositionUnshifted = getUnshiftedCameraPosition();

			updateShift();
		}

		/**
		 * Updates our shift values to try to keep |x| < 30000 and |z| < 30000, to maintain precision with cameraPosition.
		 * Since our actual range is 60000x60000, this means that we won't excessively move back and forth when moving
		 * around a chunk border.
		 */
		private void updateShift() {
			double dX = getShift(currentCameraPosition.x, previousCameraPosition.x);
			double dZ = getShift(currentCameraPosition.z, previousCameraPosition.z);

			if (dX != 0.0 || dZ != 0.0) {
				applyShift(dX, dZ);
			}
		}

		/**
		 * Shifts all current and future positions by the given amount. This is done in such a way that the difference
		 * between cameraPosition and previousCameraPosition remains the same, since they are completely arbitrary
		 * to the shader for the most part.
		 */
		private void applyShift(double dX, double dZ) {
			shift.x += dX;
			currentCameraPosition.x += dX;
			previousCameraPosition.x += dX;

			shift.z += dZ;
			currentCameraPosition.z += dZ;
			previousCameraPosition.z += dZ;
		}

		public Vector3d getCurrentCameraPosition() {
			return currentCameraPosition;
		}

		public Vector3d getPreviousCameraPosition() {
			return previousCameraPosition;
		}

		public Vector3d getPreviousCameraPositionUnshifted() {
			return previousCameraPositionUnshifted;
		}

		public double getCurrentCameraPositionY() {
			return currentCameraPosition.y;
		}
	}
}
