package org.embeddedt.embeddium.impl.gl.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    public final long pElementPointer;
    public final long pElementCount;
    public final long pBaseVertex;

    private final int capacity;

    public int size;

    public MultiDrawBatch(int capacity) {
        this.pElementPointer = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Pointer.POINTER_SIZE);
        if (this.pElementPointer == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Failed to allocate element pointer array");
        }
        MemoryUtil.memSet(this.pElementPointer, 0x0, (long) capacity * Pointer.POINTER_SIZE);

        this.pElementCount = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pElementCount == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Failed to allocate element count array");
        }
        this.pBaseVertex = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pBaseVertex == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Failed to allocate base vertex array");
        }

        this.capacity = capacity;
    }

    public int size() {
        return this.size;
    }

    public int capacity() {
        return this.capacity;
    }

    public void clear() {
        this.size = 0;
    }

    public void delete() {
        MemoryUtil.nmemAlignedFree(this.pElementPointer);
        MemoryUtil.nmemAlignedFree(this.pElementCount);
        MemoryUtil.nmemAlignedFree(this.pBaseVertex);
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }

    public int getIndexBufferSize() {
        int elements = 0;

        for (var index = 0; index < this.size; index++) {
            elements = Math.max(elements, MemoryUtil.memGetInt(this.pElementCount + ((long) index * Integer.BYTES)));
        }

        return elements;
    }
}
