package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

//? if >=1.20 {
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;

@Mixin(SpriteContents.AnimatedTexture.class)
public interface SpriteContentsAnimationAccessor {
    @Accessor("frames")
    List<SpriteContents.FrameInfo> getFrames();
}
//?}