package org.embeddedt.embeddium.impl.mixin.features.textures.animations.upload;

//? if >=1.20 {
import org.embeddedt.embeddium.impl.util.NativeImageHelper;
import org.embeddedt.embeddium.impl.mixin.features.textures.SpriteContentsInvoker;
import org.embeddedt.embeddium.api.util.ColorMixer;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;

@Mixin(SpriteContents.InterpolationData.class)
public class SpriteContentsInterpolationMixin {
    @Shadow
    @Final
    private NativeImage[] activeFrame;

    @Unique
    private SpriteContents parent;

    @Unique
    private static final int STRIDE = 4;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents parent, CallbackInfo ci) {
        this.parent = parent;
    }

    /**
     * @author JellySquid
     * @reason Drastic optimizations
     */
    @Overwrite
    void uploadInterpolatedFrame(int x, int y, SpriteContents.Ticker arg /*? if >=1.21.5-alpha.25.7.a {*//*, com.mojang.blaze3d.textures.GpuTexture tex*//*?}*/) {
        SpriteContents.AnimatedTexture animation = ((SpriteContentsAnimatorImplAccessor) arg).getAnimationInfo();
        SpriteContentsAnimationAccessor animation2 = (SpriteContentsAnimationAccessor) ((SpriteContentsAnimatorImplAccessor) arg).getAnimationInfo();
        List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor) animation).getFrames();
        SpriteContentsAnimatorImplAccessor accessor = (SpriteContentsAnimatorImplAccessor) arg;
        SpriteContentsAnimationFrameAccessor animationFrame = (SpriteContentsAnimationFrameAccessor)(Object)frames.get(accessor.getFrameIndex());

        int curIndex = animationFrame.getIndex();
        int nextIndex = ((SpriteContentsAnimationFrameAccessor)(Object)animation2.getFrames().get((accessor.getFrameIndex() + 1) % frames.size())).getIndex();

        if (curIndex == nextIndex) {
            return;
        }

        // The mix factor between the current and next frame
        float mix = 1.0F - (float) accessor.getFrameTicks() / (float) animationFrame.getTime();

        for (int layer = 0; layer < this.activeFrame.length; layer++) {
            int width = this.parent.width() >> layer;
            int height = this.parent.height() >> layer;

            int curX = ((curIndex % animation2.getFrameRowSize()) * width);
            int curY = ((curIndex / animation2.getFrameRowSize()) * height);

            int nextX = ((nextIndex % animation2.getFrameRowSize()) * width);
            int nextY = ((nextIndex / animation2.getFrameRowSize()) * height);

            NativeImage src = ((SpriteContentsAccessor) this.parent).getImages()[layer];
            NativeImage dst = this.activeFrame[layer];

            long ppSrcPixel = NativeImageHelper.getPointerRGBA(src);
            long ppDstPixel = NativeImageHelper.getPointerRGBA(dst);

            for (int layerY = 0; layerY < height; layerY++) {
                // Pointers to the pixel array for the current and next frame
                long pRgba1 = ppSrcPixel + (curX + (long) (curY + layerY) * src.getWidth()) * STRIDE;
                long pRgba2 = ppSrcPixel + (nextX + (long) (nextY + layerY) * src.getWidth()) * STRIDE;

                for (int layerX = 0; layerX < width; layerX++) {
                    int rgba1 = MemoryUtil.memGetInt(pRgba1);
                    int rgba2 = MemoryUtil.memGetInt(pRgba2);

                    // Mix the RGB components and truncate the A component
                    int mixedRgb = ColorMixer.mix(rgba1, rgba2, mix) & 0x00FFFFFF;

                    // Take the A component from the source pixel
                    int alpha = rgba1 & 0xFF000000;

                    // Update the pixel within the interpolated frame using the combined RGB and A components
                    MemoryUtil.memPutInt(ppDstPixel, mixedRgb | alpha);

                    pRgba1 += STRIDE;
                    pRgba2 += STRIDE;

                    ppDstPixel += STRIDE;
                }
            }
        }

        ((SpriteContentsInvoker) this.parent).invokeUpload(x, y, 0, 0, this.activeFrame /*? if >=1.21.5-alpha.25.7.a {*//*, tex*//*?}*/);
    }
}
//?} else if >=1.18 {
/*import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.util.ColorMixer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.NativeImage;

@Mixin(TextureAtlasSprite.InterpolationData.class)
public class SpriteContentsInterpolationMixin {
    @Shadow
    @Final
    private NativeImage[] activeFrame;

    @Unique
    private TextureAtlasSprite parent;

    @Unique
    private static final int STRIDE = 4;

    /^*
     * @author IMS
     * @reason Replace fragile Shadow
     ^/
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite parent, TextureAtlasSprite.Info pSpriteInfo, int pMipLevel, CallbackInfo ci) {
        this.parent = parent;
    }

    /^*
     * @author JellySquid
     * @reason Drastic optimizations
     ^/
    @Overwrite
    void uploadInterpolatedFrame(TextureAtlasSprite.AnimatedTexture animation) {
        TextureAtlasSprite.FrameInfo animationFrame = animation.frames.get(animation.frame);

        int curIndex = animationFrame.index;
        int nextIndex = animation.frames.get((animation.frame + 1) % animation.frames.size()).index;

        if (curIndex == nextIndex) {
            return;
        }

        float delta = 1.0F - (float) animation.subFrame / (float) animationFrame.time;

        for (int layer = 0; layer < this.activeFrame.length; layer++) {
            int width = this.parent.width >> layer;
            int height = this.parent.height >> layer;

            int curX = ((curIndex % animation.frameRowSize) * width);
            int curY = ((curIndex / animation.frameRowSize) * height);

            int nextX = ((nextIndex % animation.frameRowSize) * width);
            int nextY = ((nextIndex / animation.frameRowSize) * height);

            NativeImage src = this.parent.mainImage[layer];
            NativeImage dst = this.activeFrame[layer];

            // Destination pointers
            long dp = dst.pixels;

            for (int layerY = 0; layerY < height; layerY++) {
                // Source pointers
                long s1p = src.pixels + (curX + ((long) (curY + layerY) * src.getWidth())) * STRIDE;
                long s2p = src.pixels + (nextX + ((long) (nextY + layerY) * src.getWidth())) * STRIDE;

                for (int layerX = 0; layerX < width; layerX++) {
                    int colorA = MemoryUtil.memGetInt(s1p);
                    int colorB = MemoryUtil.memGetInt(s2p);
                    int colorMixed = ColorMixer.mix(colorA, colorB, delta) & 0x00FFFFFF;
                    // Use alpha from first color as-is, do not blend
                    MemoryUtil.memPutInt(dp, colorMixed | (colorA & 0xFF000000));

                    s1p += STRIDE;
                    s2p += STRIDE;
                    dp += STRIDE;
                }
            }
        }

        this.parent.upload(0, 0, this.activeFrame);
    }
}
*///?}