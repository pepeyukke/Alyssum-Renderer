package org.embeddedt.embeddium.impl.model.color.interop;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;

public interface BlockColorsExtended {
    static Reference2ReferenceMap<Block, BlockColor> getProviders(BlockColors blockColors) {
        return ((BlockColorsExtended) blockColors).sodium$getProviders();
    }

    static ReferenceSet<Block> getOverridenVanillaBlocks(BlockColors blockColors) {
        return ((BlockColorsExtended) blockColors).embeddium$getOverridenVanillaBlocks();
    }

    Reference2ReferenceMap<Block, BlockColor> sodium$getProviders();

    ReferenceSet<Block> embeddium$getOverridenVanillaBlocks();
}
