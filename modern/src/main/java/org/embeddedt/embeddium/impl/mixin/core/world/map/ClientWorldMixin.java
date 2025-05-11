package org.embeddedt.embeddium.impl.mixin.core.world.map;

import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientLevel.class)
public class ClientWorldMixin implements ChunkTrackerHolder {
    @Unique
    private final ChunkTracker chunkTracker = new ChunkTracker();

    @Override
    public ChunkTracker sodium$getTracker() {
        return Validate.notNull(this.chunkTracker);
    }

    //? if >=1.17 {
    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void markLoaded(ChunkPos pChunkPos, CallbackInfo ci) {
        this.chunkTracker.onChunkStatusAdded(pChunkPos.x, pChunkPos.z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }
    //?} else {
    /*@Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void markLoaded(int x, int z, CallbackInfo ci) {
        this.chunkTracker.onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }
    *///?}

    @Inject(method = "unload", at = @At("RETURN"))
    private void markUnloaded(LevelChunk chunk, CallbackInfo ci) {
        this.chunkTracker.onChunkStatusRemoved(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }
}
