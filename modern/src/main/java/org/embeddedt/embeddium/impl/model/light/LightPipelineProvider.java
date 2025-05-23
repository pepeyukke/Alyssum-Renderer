package org.embeddedt.embeddium.impl.model.light;

import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.flat.FlatLightPipeline;
import org.embeddedt.embeddium.impl.model.light.smooth.SmoothLightPipeline;

import java.util.EnumMap;

/**
 * Contains the quad lighters that are used to compute lightmap & brightness data for each quad. On Forge, when the
 * experimental light pipeline is enabled, a passthrough implementation is used that has Forge's QuadLighter do the
 * lighting. Otherwise, the built-in {@link SmoothLightPipeline} and {@link FlatLightPipeline}, which implement
 * a lighting model that is very similar logic to vanilla, but has several optimizations & fixes some visual issues.
 */
public class LightPipelineProvider {
    private final EnumMap<LightMode, LightPipeline> lighters = new EnumMap<>(LightMode.class);
    private final LightDataAccess lightData;

    public LightPipelineProvider(LightDataAccess cache, boolean enableDirectionalShading) {
        this.lightData = cache;
        if (LoaderServices.INSTANCE.hasCustomLightPipeline()) {
            this.lighters.put(LightMode.SMOOTH, LoaderServices.INSTANCE.createCustomLightPipeline(LightMode.SMOOTH, cache));
            this.lighters.put(LightMode.FLAT, LoaderServices.INSTANCE.createCustomLightPipeline(LightMode.FLAT, cache));
        } else {
            this.lighters.put(LightMode.SMOOTH, new SmoothLightPipeline(cache, enableDirectionalShading));
            this.lighters.put(LightMode.FLAT, new FlatLightPipeline(cache, enableDirectionalShading));
        }
    }

    public LightPipeline getLighter(LightMode type) {
        LightPipeline pipeline = this.lighters.get(type);

        if (pipeline == null) {
            throw new NullPointerException("No lighter exists for mode: " + type.name());
        }

        return pipeline;
    }

    public LightDataAccess getLightData() {
        return this.lightData;
    }

    /**
     * Reset the light pipelines. This should be called whenever the underlying world data has changed to invalidate
     * any caches.
     */
    public void reset() {
        for (LightPipeline pipeline : this.lighters.values()) {
            pipeline.reset();
        }
    }
}
