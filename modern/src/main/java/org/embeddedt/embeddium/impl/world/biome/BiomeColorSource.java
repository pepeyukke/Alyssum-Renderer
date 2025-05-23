package org.embeddedt.embeddium.impl.world.biome;

import net.minecraft.client.renderer.BiomeColors;
//? if >=1.15 {
import net.minecraft.world.level.ColorResolver;
 //?} else
/*import net.minecraft.client.renderer.BiomeColors.ColorResolver;*/

public enum BiomeColorSource {
    GRASS,
    FOLIAGE,
    WATER;

    public static final BiomeColorSource[] VALUES = BiomeColorSource.values();
    public static final int COUNT = VALUES.length;

    public static BiomeColorSource from(ColorResolver resolver) {
        if (resolver == BiomeColors.GRASS_COLOR_RESOLVER) {
            return GRASS;
        } else if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) {
            return FOLIAGE;
        } else if (resolver == BiomeColors.WATER_COLOR_RESOLVER) {
            return WATER;
        }

        return null;
    }
}
