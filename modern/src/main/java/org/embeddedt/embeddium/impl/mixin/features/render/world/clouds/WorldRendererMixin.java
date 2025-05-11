package org.embeddedt.embeddium.impl.mixin.features.render.world.clouds;

//? if >=1.17 <1.21.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.embeddedt.embeddium.impl.render.immediate.CloudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
//? if >=1.20 {
import org.joml.Matrix4f;
//?} else
/*import com.mojang.math.Matrix4f;*/
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 990)
public class WorldRendererMixin {
    @Shadow
    private @Nullable ClientLevel level;
    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private CloudRenderer cloudRenderer;

    /**
     * @author jellysquid3
     * @reason Optimize cloud rendering
     */
    @Inject(method = "renderClouds", at = @At(value = "INVOKE", target = "Ljava/lang/Float;isNaN(F)Z", ordinal = 0), cancellable = true)
    public void renderCloudsFast(PoseStack matrices, /*? if >=1.20.6 {*/ /*Matrix4f modelViewMatrix, *//*?}*/ Matrix4f projectionMatrix, float tickDelta, double x, double y, double z, CallbackInfo ci) {
        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CloudRenderer(this.minecraft.getResourceManager());
        }

        boolean renderFasterClouds = Celeritas.options().performance.useFasterClouds;

        if (renderFasterClouds) {
            //? if <1.20.6 {
            Matrix4f modelViewMatrix = matrices.last().pose();
            //?}
            //? if >=1.19.3 {
            this.cloudRenderer.render(this.level, this.minecraft.player, modelViewMatrix, projectionMatrix, this.ticks, tickDelta, x, y, z);
            //?} else {
            /*this.cloudRenderer.render(this.level, this.minecraft.player, JomlHelper.copy(modelViewMatrix), JomlHelper.copy(projectionMatrix), this.ticks, tickDelta, x, y, z);
            *///?}
            ci.cancel();
        }
    }

    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("RETURN"))
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.reloadTextures(manager);
        }
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        // will be re-allocated on next use
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }

    @Inject(method = "close", at = @At("RETURN"), remap = false)
    private void onClose(CallbackInfo ci) {
        // will never be re-allocated, as the renderer is shutting down
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }
}
//?}