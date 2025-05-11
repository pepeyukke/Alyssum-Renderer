package net.irisshaders.iris.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;
import org.taumc.celeritas.CeleritasShaderVersionService;

public class PipelineManager {
	private final Function<NamespacedId, WorldRenderingPipeline> pipelineFactory;
	private final Map<NamespacedId, WorldRenderingPipeline> pipelinesPerDimension = new HashMap<>();
	private WorldRenderingPipeline pipeline = CeleritasShaderVersionService.INSTANCE.createVanillaRenderingPipeline();
	private int versionCounterForSodiumShaderReload = 0;

	public PipelineManager(Function<NamespacedId, WorldRenderingPipeline> pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	public WorldRenderingPipeline preparePipeline(NamespacedId currentDimension) {
		if (!pipelinesPerDimension.containsKey(currentDimension)) {
			SystemTimeUniforms.COUNTER.reset();
			SystemTimeUniforms.TIMER.reset();

			IRIS_LOGGER.info("Creating pipeline for dimension {}", currentDimension);
			pipeline = pipelineFactory.apply(currentDimension);
			pipelinesPerDimension.put(currentDimension, pipeline);

			WorldRenderingSettings.INSTANCE.reloadRendererIfRequired();
		} else {
			pipeline = pipelinesPerDimension.get(currentDimension);
		}

		return pipeline;
	}

	@Nullable
	public WorldRenderingPipeline getPipelineNullable() {
		return pipeline;
	}

	public Optional<WorldRenderingPipeline> getPipeline() {
		return Optional.ofNullable(pipeline);
	}

	/**
	 * In IrisChunkProgramOverrides#getProgramOverride,
	 * it uses version counter to check whether to reload sodium shaders.
	 * This fixes a compat issue with Immersive Portals(#1188).
	 * Immersive Portals may load multiple client dimensions at the same time,
	 * and every dimension corresponds to a IrisChunkProgramOverrides object.
	 * Multiple dimensions (mod dimensions that fallback to overworld shaders) may use the same pipeline.
	 * This ensures that the sodium shader for each dimension will get properly reloaded.
	 */
	public int getVersionCounterForSodiumShaderReload() {
		return versionCounterForSodiumShaderReload;
	}

	/**
	 * Destroys all the current pipelines.
	 *
	 * <p>This method is <b>EXTREMELY DANGEROUS!</b> It is a huge potential source of hard-to-trace inconsistencies
	 * in program state. You must make sure that you <i>immediately</i> re-prepare the pipeline after destroying
	 * it to prevent the program from falling into an inconsistent state.</p>
	 *
	 * <p>In particular, </p>
	 *
	 * @see <a href="https://github.com/IrisShaders/Iris/issues/1330">this GitHub issue</a>
	 */
	public void destroyPipeline() {
		pipelinesPerDimension.forEach((dimensionId, pipeline) -> {
			IRIS_LOGGER.info("Destroying pipeline {}", dimensionId);
			resetTextureState();
			pipeline.destroy();
		});

		pipelinesPerDimension.clear();
		pipeline = null;
		versionCounterForSodiumShaderReload++;
	}

	private void resetTextureState() {
		// Unbind all textures
		//
		// This is necessary because we don't want destroyed render target textures to remain bound to certain texture
		// units. Vanilla appears to properly rebind all textures as needed, and we do so too, so this does not cause
		// issues elsewhere.
		//
		// Without this code, there will be weird issues when reloading certain shaderpacks.
		for (int i = 0; i < 16; i++) {
			GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE0 + i);
			GL_STATE_MANAGER.bindTexture(0);
		}

		// Set the active texture unit to unit 0
		//
		// This seems to be what most code expects. It's a sane default in any case.
		GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE0);
	}
}
