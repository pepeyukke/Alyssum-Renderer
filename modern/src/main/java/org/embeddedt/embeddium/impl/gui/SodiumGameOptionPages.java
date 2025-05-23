package org.embeddedt.embeddium.impl.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
//? if forge
import net.minecraftforge.common.ForgeConfig;
//? if <1.19
/*import net.minecraft.client.Option;*/
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.compat.modernui.MuiGuiScaleHook;
//? if >=1.18
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;
import org.embeddedt.embeddium.impl.gl.arena.staging.MappedStagingBuffer;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.api.options.binding.compat.VanillaBooleanOptionBinding;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.storage.MinecraftOptionsStorage;
import org.embeddedt.embeddium.impl.gui.options.storage.SodiumOptionsStorage;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
//? if >=1.21.2
/*import net.minecraft.server.level.ParticleStatus;*/
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.gui.options.FullscreenResolutionHelper;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    private static int computeMaxRangeForRenderDistance(@SuppressWarnings("SameParameterValue") int injectedRenderDistance) {
        //? if >=1.19 {
        if(vanillaOpts.getData().renderDistance().values() instanceof OptionInstance.IntRange range) {
            injectedRenderDistance = Math.max(injectedRenderDistance, range.maxInclusive());
        }
        //?}
        return injectedRenderDistance;
    }

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.RENDER_DISTANCE)
                        .setName(ComponentUtil.translatable("options.renderDistance"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, computeMaxRangeForRenderDistance(32), 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.renderDistance/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), options -> options.renderDistance/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if >=1.18 {
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.SIMULATION_DISTANCE)
                        .setName(ComponentUtil.translatable("options.simulationDistance"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.simulation_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.simulationDistance/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), options -> options.simulationDistance/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //?}
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BRIGHTNESS)
                        .setName(ComponentUtil.translatable("options.gamma"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value * 0.01D), (opts) -> (int) (opts.gamma/*? if >=1.19 {*/().get()/*?}*/ / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.WINDOW)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.GUI_SCALE)
                        .setName(ComponentUtil.translatable("options.guiScale"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, MuiGuiScaleHook.getMaxGuiScale(), 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);

                            Minecraft client = Minecraft.getInstance();
                            client.resizeDisplay();
                        }, opts -> opts.guiScale/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.FULLSCREEN)
                        .setName(ComponentUtil.translatable("options.fullscreen"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);

                            Minecraft client = Minecraft.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen/*? if >=1.19 {*/().get()/*?}*/) {
                                window.toggleFullScreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(window.isFullscreen());
                            }
                        }, (opts) -> opts.fullscreen/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                .addConditionally(!FullscreenResolutionHelper.isFullscreenResAlreadyAdded(), FullscreenResolutionHelper::createFullScreenResolutionOption)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VSYNC)
                        .setName(ComponentUtil.translatable("options.vsync"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        //? if >=1.19 {
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.enableVsync()))
                        //?} else
                        /*.setBinding(new VanillaBooleanOptionBinding(Option.ENABLE_VSYNC))*/
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MAX_FRAMERATE)
                        .setName(ComponentUtil.translatable("options.framerateLimit"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.framerateLimit/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);
                            //? if <1.21.2
                            Minecraft.getInstance().getWindow().setFramerateLimit(value);
                            //? if >=1.21.2
                            /*Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(value);*/
                        }, opts -> opts.framerateLimit/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                //? if >=1.21.2 {
                /*.add(OptionImpl.createBuilder(InactivityFpsLimit.class, vanillaOpts)
                        .setId(StandardOptions.Option.INACTIVITY_FPS_LIMIT)
                        .setName(ComponentUtil.translatable("options.inactivityFpsLimit"))
                        .setTooltip(ComponentUtil.translatable("embeddium.options.inactivity_fps_limit.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, InactivityFpsLimit.class, Arrays.stream(InactivityFpsLimit.values()).map(InactivityFpsLimit::getKey).map(Component::translatable).toArray(Component[]::new)))
                        .setBinding((opts, value) -> {
                            opts.inactivityFpsLimit().set(value);
                        }, opts -> opts.inactivityFpsLimit().get())
                        .build())
                *///?}
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.INDICATORS)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VIEW_BOBBING)
                        .setName(ComponentUtil.translatable("options.viewBobbing"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        //? if >=1.19 {
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.bobView()))
                        //?} else
                        /*.setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))*/
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicatorStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.ATTACK_INDICATOR)
                        .setName(ComponentUtil.translatable("options.attackIndicator"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicatorStatus.class, new Component[] { ComponentUtil.translatable("options.off"), ComponentUtil.translatable("options.attack.crosshair"), ComponentUtil.translatable("options.attack.hotbar") }))
                        .setBinding((opts, value) -> opts.attackIndicator/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), (opts) -> opts.attackIndicator/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                //? if >=1.18 {
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.AUTOSAVE_INDICATOR)
                        .setName(ComponentUtil.translatable("options.autosaveIndicator"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.autosave_indicator.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.showAutosaveIndicator/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.showAutosaveIndicator/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                //?}
                .build());

        return new OptionPage(ComponentUtil.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.GRAPHICS)
                .add(OptionImpl.createBuilder(/*? if >=1.16 {*/ GraphicsStatus.class /*?} else {*/ /*boolean.class *//*?}*/, vanillaOpts)
                        .setId(StandardOptions.Option.GRAPHICS_MODE)
                        .setName(ComponentUtil.translatable("options.graphics"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.graphics_quality.tooltip"))
                        //? if >=1.16 {
                        .setControl(option -> new CyclingControl<>(option, GraphicsStatus.class, new Component[] { ComponentUtil.translatable("options.graphics.fast"), ComponentUtil.translatable("options.graphics.fancy"), ComponentUtil.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC) }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value),
                                opts -> opts.graphicsMode/*? if >=1.19 {*/().get()/*?}*/)
                        //?} else {
                        /*.setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.fancyGraphics = value, opts -> opts.fancyGraphics)
                        *///?}
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.DETAILS)
                .add(OptionImpl.createBuilder(CloudStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.CLOUDS)
                        .setName(ComponentUtil.translatable("options.renderClouds"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, CloudStatus.class, new Component[] { ComponentUtil.translatable("options.off"), ComponentUtil.translatable("options.graphics.fast"), ComponentUtil.translatable("options.graphics.fancy") }))
                        .setBinding((opts, value) -> {
                            //? if >=1.19 {
                            opts.cloudStatus().set(value);
                            //?} else {
                            /*opts.renderClouds = value;
                            *///?}

                            //? if >=1.16 {
                            if (Minecraft.useShaderTransparency()) {
                                RenderTarget framebuffer = Minecraft.getInstance().levelRenderer.getCloudsTarget();
                                if (framebuffer != null) {
                                    framebuffer.clear(/*? if <1.21.2 {*/Minecraft.ON_OSX/*?}*/);
                                }
                            }
                            //?}
                        }, opts -> {
                            //? if >=1.19 {
                            return opts.cloudStatus().get();
                            //?} else {
                            /*return opts.renderClouds;
                            *///?}
                        })
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.WEATHER)
                        .setName(ComponentUtil.translatable("soundCategory.weather"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.LEAVES)
                        .setName(ComponentUtil.translatable("sodium.options.leaves_quality.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.leaves_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(ParticleStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.PARTICLES)
                        .setName(ComponentUtil.translatable("options.particles"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticleStatus.class, new Component[] { ComponentUtil.translatable("options.particles.all"), ComponentUtil.translatable("options.particles.decreased"), ComponentUtil.translatable("options.particles.minimal") }))
                        .setBinding((opts, value) -> opts.particles/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), (opts) -> opts.particles/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.SMOOTH_LIGHT)
                        .setName(ComponentUtil.translatable("options.ao"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.smooth_lighting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.ambientOcclusion/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(/*? if <1.20 {*/  /*value ? AmbientOcclusionStatus.MAX : AmbientOcclusionStatus.OFF *//*?} else {*/ value /*?}*/), opts -> opts.ambientOcclusion/*? if >=1.19 {*/().get()/*?}*/ /*? if <1.20 {*/ /*!= AmbientOcclusionStatus.OFF *//*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BIOME_BLEND)
                        .setName(ComponentUtil.translatable("options.biomeBlendRadius"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.biomeBlend()))
                        .setBinding((opts, value) -> opts.biomeBlendRadius/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.biomeBlendRadius/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if >=1.16 {
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_DISTANCE)
                        .setName(ComponentUtil.translatable("options.entityDistanceScaling"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> {
                            //? if >=1.19 {
                            opts.entityDistanceScaling().set(value / 100.0);
                            //?} else
                            /*opts.entityDistanceScaling = value / 100.0f;*/
                        }, opts -> Math.round(opts.entityDistanceScaling/*? if >=1.19 {*/().get().floatValue()/*?}*/ * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                //?}
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_SHADOWS)
                        .setName(ComponentUtil.translatable("options.entityShadows"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.entityShadows/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.VIGNETTE)
                        .setName(ComponentUtil.translatable("sodium.options.vignette.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.MIPMAPS)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MIPMAP_LEVEL)
                        .setName(ComponentUtil.translatable("options.mipmapLevels"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.mipmapLevels/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.SORTING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.TRANSLUCENT_FACE_SORTING)
                        .setName(ComponentUtil.translatable("sodium.options.translucent_face_sorting.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.translucent_face_sorting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.VARIES)
                        .setBinding((opts, value) -> opts.performance.useTranslucentFaceSorting = value, opts -> opts.performance.useTranslucentFaceSorting)
                        .setEnabled(!ShaderModBridge.isNvidiumEnabled())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.LIGHTING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.USE_QUAD_NORMALS_FOR_LIGHTING)
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.quality.useQuadNormalsForShading = value, opts -> opts.quality.useQuadNormalsForShading)
                        //? if forge
                        .setEnabled(!ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionPage performance() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CHUNK_UPDATES)
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CHUNK_UPDATE_THREADS)
                        .setName(ComponentUtil.translatable("sodium.options.chunk_update_threads.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.chunk_update_threads.tooltip"))
                        .setControl(o -> new SliderControl(o, 0, ChunkBuilder.getMaxThreadCount(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default")))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.DEFFER_CHUNK_UPDATES)
                        .setName(ComponentUtil.translatable("sodium.options.always_defer_chunk_updates.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.always_defer_chunk_updates.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build())
                .add(OptionImpl.createBuilder(AsyncOcclusionMode.class, sodiumOpts)
                        .setId(StandardOptions.Option.ASYNC_GRAPH_SEARCH)
                        .setName(ComponentUtil.translatable("celeritas.options.async_graph_search.name"))
                        .setTooltip(ComponentUtil.translatable("celeritas.options.async_graph_search.tooltip"))
                        .setControl(o -> new CyclingControl<>(o, AsyncOcclusionMode.class, new Component[] { ComponentUtil.literal("Off"), ComponentUtil.literal("Only Shadows"), ComponentUtil.literal("Everything") }))
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.asyncOcclusionMode = value, opts -> opts.performance.asyncOcclusionMode)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build()
        );

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING_CULLING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.BLOCK_FACE_CULLING)
                        .setName(ComponentUtil.translatable("sodium.options.use_block_face_culling.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_block_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.COMPACT_VERTEX_FORMAT)
                        .setName(ComponentUtil.translatable("sodium.options.use_compact_vertex_format.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_compact_vertex_format.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setEnabled(!ShaderModBridge.areShadersEnabled())
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> {
                            opts.performance.useCompactVertexFormat = value;
                        }, opts -> opts.performance.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.FOG_OCCLUSION)
                        .setName(ComponentUtil.translatable("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ENTITY_CULLING)
                        .setName(ComponentUtil.translatable("sodium.options.use_entity_culling.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ANIMATE_VISIBLE_TEXTURES)
                        .setName(ComponentUtil.translatable("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_CONSOLIDATION)
                        .setName(ComponentUtil.translatable("embeddium.options.use_render_pass_consolidation.name"))
                        .setTooltip(ComponentUtil.translatable("embeddium.options.use_render_pass_consolidation.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassConsolidation = value, opts -> opts.performance.useRenderPassConsolidation)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_OPTIMIZATION)
                        .setName(ComponentUtil.translatable("embeddium.options.use_render_pass_optimization.name"))
                        .setTooltip(ComponentUtil.translatable("embeddium.options.use_render_pass_optimization.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassOptimization = value, opts -> opts.performance.useRenderPassOptimization)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if <1.21.2 {
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.USE_FASTER_CLOUDS)
                        .setName(ComponentUtil.translatable("embeddium.options.use_faster_clouds.name"))
                        .setTooltip(ComponentUtil.translatable("embeddium.options.use_faster_clouds.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useFasterClouds = value, opts -> opts.performance.useFasterClouds)
                        .build())
                //?}
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.NO_ERROR_CONTEXT)
                        .setName(ComponentUtil.translatable("sodium.options.use_no_error_context.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_no_error_context.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useNoErrorGLContext = value, opts -> opts.performance.useNoErrorGLContext)
                        .setEnabled(supportsNoErrorContext())
                        .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                        .build())
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.performance"), ImmutableList.copyOf(groups));
    }

    private static boolean supportsNoErrorContext() {
        //? if >=1.18 {
        GLCapabilities capabilities = GL.getCapabilities();
        return (capabilities.OpenGL46 || capabilities.GL_KHR_no_error)
                && !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
        //?} else
        /*return false;*/
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CPU_SAVING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.PERSISTENT_MAPPING)
                        .setName(ComponentUtil.translatable("sodium.options.use_persistent_mapping.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_persistent_mapping.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(MappedStagingBuffer.isSupported(RenderDevice.INSTANCE))
                        .setBinding((opts, value) -> opts.advanced.useAdvancedStagingBuffers = value, opts -> opts.advanced.useAdvancedStagingBuffers)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CPU_FRAMES_AHEAD)
                        .setName(ComponentUtil.translatable("sodium.options.cpu_render_ahead_limit.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.translateVariable("sodium.options.cpu_render_ahead_limit.value")))
                        .setBinding((opts, value) -> opts.advanced.cpuRenderAheadLimit = value, opts -> opts.advanced.cpuRenderAheadLimit)
                        .build()
                )
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }

    public static OptionStorage<Options> getVanillaOpts() {
        return vanillaOpts;
    }

    public static OptionStorage<SodiumGameOptions> getSodiumOpts() {
        return sodiumOpts;
    }
}
