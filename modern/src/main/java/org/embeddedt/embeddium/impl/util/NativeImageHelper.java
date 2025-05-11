package org.embeddedt.embeddium.impl.util;

import org.embeddedt.embeddium.impl.mixin.features.textures.NativeImageAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Locale;

public class NativeImageHelper {
    public static long getPointerRGBA(NativeImage nativeImage) {
        if (nativeImage.format() != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "Tried to get pointer to RGBA pixel data on NativeImage of wrong format; have %s", nativeImage.format()));
        }

        return ((NativeImageAccessor) (Object) nativeImage) // duck type since NativeImage is final
                .getPointer();
    }
}
