package org.embeddedt.embeddium.impl.mixin.core.world.chunk;

import net.minecraft.core.IdMapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.Palette;
import org.embeddedt.embeddium.impl.world.PaletteStorageExtended;
import org.embeddedt.embeddium.impl.world.ReadableContainerExtended;
//? if <1.18
/*import org.embeddedt.embeddium.impl.world.ClonedPalette;*/
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.function.Function;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> implements ReadableContainerExtended<T> {

    //? if >=1.18 {
    @Shadow
    private volatile PalettedContainer.Data<T> data;

    @Shadow
    @Final
    private PalettedContainer.Strategy strategy;

    @Shadow
    public abstract PalettedContainer<T> copy();

    @Override
    public void sodium$unpack(T[] values) {
        var indexer = Objects.requireNonNull(this.strategy);

        if (values.length != indexer.size()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = (PaletteStorageExtended) data.storage();
        storage.sodium$unpack(values, data.palette(), null);
    }

    @Override
    public void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var indexer = Objects.requireNonNull(this.strategy);

        if (values.length != indexer.size()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = data.storage();
        var palette = data.palette();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int localBlockIndex = indexer.getIndex(x, y, z);

                    int paletteIndex = storage.get(localBlockIndex);
                    var paletteValue =  palette.valueFor(paletteIndex);

                    values[localBlockIndex] = Objects.requireNonNull(paletteValue, "Palette does not contain entry for value in storage");
                }
            }
        }
    }

    @Override
    public PalettedContainer<T> sodium$copy() {
        return this.copy();
    }
    //?} else {
    /*@Shadow
    protected BitStorage storage;

    @Shadow
    private Palette<T> palette;

    @Shadow
    @Final
    private T defaultValue;

    @Shadow
    private static int getIndex(int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    private IdMapper<T> registry;

    @Shadow
    @Final
    private Palette<T> globalPalette;

    @Shadow
    @Final
    private Function<T, CompoundTag> writer;

    @Shadow
    @Final
    private Function<CompoundTag, T> reader;

    @Shadow
    private int bits;

    @Override
    public void sodium$unpack(T[] values) {
        if (values.length != 4096) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var storage = (PaletteStorageExtended) Objects.requireNonNull(this.storage, "PalettedContainer must have storage");
        var palette = Objects.requireNonNull(this.palette, "PalettedContainer must have palette");

        storage.sodium$unpack(values, palette, this.defaultValue);
    }

    @Override
    public void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (values.length != 4096) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var storage = Objects.requireNonNull(this.storage, "PalettedContainer must have storage");
        var palette = Objects.requireNonNull(this.palette, "PalettedContainer must have palette");
        var defaultValue = this.defaultValue;

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int localBlockIndex = getIndex(x, y, z);

                    int paletteIndex = storage.get(localBlockIndex);
                    var paletteValue =  palette.valueFor(paletteIndex);

                    values[localBlockIndex] = Objects.requireNonNullElse(paletteValue, defaultValue);
                }
            }
        }
    }

    @Override
    public PalettedContainer<T> sodium$copy() {
        PalettedContainer<T> container = new PalettedContainer<>(this.globalPalette, this.registry, this.reader, this.writer, this.defaultValue);
        ((PalettedContainerMixin<T>)(Object)container).storage = new BitStorage(this.bits, 4096, this.storage.getRaw().clone());
        ((PalettedContainerMixin<T>)(Object)container).bits = this.bits;
        if (this.palette instanceof GlobalPalette<T>) {
            ((PalettedContainerMixin<T>)(Object)container).palette = this.palette;
        } else {
            ((PalettedContainerMixin<T>)(Object)container).palette = new ClonedPalette<>(this.palette, this.bits);
        }
        return container;
    }
    *///?}
}
