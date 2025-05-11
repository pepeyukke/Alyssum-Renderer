package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

//? if >=1.20 {
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.FrameInfo.class)
public interface SpriteContentsAnimationFrameAccessor {
    @Accessor("time")
    int getTime();
}
//?}