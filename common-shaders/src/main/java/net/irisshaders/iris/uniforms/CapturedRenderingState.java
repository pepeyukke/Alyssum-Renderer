package net.irisshaders.iris.uniforms;

import org.joml.Matrix4f;
import org.joml.Vector3d;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class CapturedRenderingState {
	public static final CapturedRenderingState INSTANCE = new CapturedRenderingState();

	private static final Vector3d ZERO_VECTOR_3d = new Vector3d();

    // Used for 1.7.10
    private final Vector3d cameraPosition = new Vector3d();
    private float fov;

	private Matrix4f gbufferModelView;
	private Matrix4f gbufferProjection;
	private Vector3d fogColor;
	private float fogDensity;
	private float darknessLightFactor;
	private float tickDelta;
	private float realTickDelta;
	private int currentRenderedBlockEntity;

	private int currentRenderedEntity = -1;
	private int currentRenderedItem = -1;

	private float currentAlphaTest;
	private float cloudTime;

	private CapturedRenderingState() {
	}


    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public void setCameraPosition(double x, double y, double z) {
        cameraPosition.set(x, y, z);
    }
    public Vector3d getCameraPosition() {
        return cameraPosition;
    }

	public Matrix4f getGbufferModelView() {
		return gbufferModelView;
	}

	public void setGbufferModelView(Matrix4f gbufferModelView) {
		this.gbufferModelView = new Matrix4f(gbufferModelView);
	}

	public Matrix4f getGbufferProjection() {
		return gbufferProjection;
	}

	public void setGbufferProjection(Matrix4f gbufferProjection) {
		this.gbufferProjection = new Matrix4f(gbufferProjection);
	}

	public Vector3d getFogColor() {
		if (!MINECRAFT_SHIM.isLevelLoaded() || fogColor == null) {
			return ZERO_VECTOR_3d;
		}

		return fogColor;
	}

	public void setFogColor(float red, float green, float blue) {
		fogColor = new Vector3d(red, green, blue);
	}

	public float getFogDensity() {
		return fogDensity;
	}

	public void setFogDensity(float fogDensity) {
		this.fogDensity = fogDensity;
	}

	public float getTickDelta() {
		return tickDelta;
	}

	public void setTickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public float getRealTickDelta() {
		return realTickDelta;
	}

	public void setRealTickDelta(float tickDelta) {
		this.realTickDelta = tickDelta;
	}

	public void setCurrentBlockEntity(int entity) {
		this.currentRenderedBlockEntity = entity;
	}

	public int getCurrentRenderedBlockEntity() {
		return currentRenderedBlockEntity;
	}

	public void setCurrentEntity(int entity) {
		this.currentRenderedEntity = entity;
	}

	public int getCurrentRenderedEntity() {
		return currentRenderedEntity;
	}

	public int getCurrentRenderedItem() {
		return currentRenderedItem;
	}

	public void setCurrentRenderedItem(int item) {
		this.currentRenderedItem = item;
	}

	public float getCurrentAlphaTest() {
		return currentAlphaTest;
	}

	public void setCurrentAlphaTest(float alphaTest) {
		this.currentAlphaTest = alphaTest;
	}

	public float getDarknessLightFactor() {
		return darknessLightFactor;
	}

	public void setDarknessLightFactor(float factor) {
		darknessLightFactor = factor;
	}

	public float getCloudTime() {
		return this.cloudTime;
	}

	public void setCloudTime(float cloudTime) {
		this.cloudTime = cloudTime;
	}
}
