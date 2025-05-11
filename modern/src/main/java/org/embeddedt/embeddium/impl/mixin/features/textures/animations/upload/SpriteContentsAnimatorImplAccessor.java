package org.embeddedt.embeddium.impl.mixin.features.textures.animations.upload;

//? if >=1.20 {
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.Ticker.class)
public interface SpriteContentsAnimatorImplAccessor {
    @Accessor
    SpriteContents.AnimatedTexture getAnimationInfo();

    @Accessor("frame")
    int getFrameIndex();

    @Accessor("subFrame")
    int getFrameTicks();
}
//?}