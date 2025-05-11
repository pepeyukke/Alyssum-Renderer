package org.embeddedt.embeddium.impl.render.chunk.compile.sorting;

import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.embeddedt.embeddium.impl.util.sorting.MergeSort;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class QuadPrimitiveType implements ChunkPrimitiveType {
    public static final QuadPrimitiveType INSTANCE = new QuadPrimitiveType();

    private static final int ELEMENTS_PER_PRIMITIVE = 6;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    private static final int FAKE_STATIC_CAMERA_OFFSET = 1000;

    @Override
    public int getIndexBufferElementsPerPrimitive() {
        return ELEMENTS_PER_PRIMITIVE;
    }

    @Override
    public int getVerticesPerPrimitive() {
        return VERTICES_PER_PRIMITIVE;
    }

    @Override
    public void generateSimpleIndexBuffer(ByteBuffer indexBuffer, int numPrimitives) {
        int minimumRequiredBufferSize = getIndexBufferSize(numPrimitives);
        if(indexBuffer.capacity() < minimumRequiredBufferSize) {
            throw new IllegalStateException("Given index buffer has length " + indexBuffer.capacity() + " but we need " + minimumRequiredBufferSize);
        }
        long ptr = MemoryUtil.memAddress(indexBuffer);

        for (int primitiveIndex = 0; primitiveIndex < numPrimitives; primitiveIndex++) {
            int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
            int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

            MemoryUtil.memPutInt(ptr + (indexOffset + 0) * 4, vertexOffset + 0);
            MemoryUtil.memPutInt(ptr + (indexOffset + 1) * 4, vertexOffset + 1);
            MemoryUtil.memPutInt(ptr + (indexOffset + 2) * 4, vertexOffset + 2);

            MemoryUtil.memPutInt(ptr + (indexOffset + 3) * 4, vertexOffset + 2);
            MemoryUtil.memPutInt(ptr + (indexOffset + 4) * 4, vertexOffset + 3);
            MemoryUtil.memPutInt(ptr + (indexOffset + 5) * 4, vertexOffset + 0);
        }
    }

    private void generateIndexBuffer(ByteBuffer indexBuffer, int[] primitiveMapping) {
        int bufferSize = getIndexBufferSize(primitiveMapping.length);
        if(indexBuffer.capacity() != bufferSize) {
            throw new IllegalStateException("Given index buffer has length " + indexBuffer.capacity() + " but we expected " + bufferSize);
        }
        long ptr = MemoryUtil.memAddress(indexBuffer);

        for (int primitiveIndex = 0; primitiveIndex < primitiveMapping.length; primitiveIndex++) {
            int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;

            // Map to the desired primitive
            int vertexOffset = primitiveMapping[primitiveIndex] * VERTICES_PER_PRIMITIVE;

            MemoryUtil.memPutInt(ptr + (indexOffset + 0) * 4, vertexOffset + 0);
            MemoryUtil.memPutInt(ptr + (indexOffset + 1) * 4, vertexOffset + 1);
            MemoryUtil.memPutInt(ptr + (indexOffset + 2) * 4, vertexOffset + 2);

            MemoryUtil.memPutInt(ptr + (indexOffset + 3) * 4, vertexOffset + 2);
            MemoryUtil.memPutInt(ptr + (indexOffset + 4) * 4, vertexOffset + 3);
            MemoryUtil.memPutInt(ptr + (indexOffset + 5) * 4, vertexOffset + 0);
        }
    }

    private static void buildStaticDistanceArray(float[] centers, float[] distanceArray, float x, float y, float z,
                                                 float normX, float normY, float normZ, int quadCount, BitSet normalSigns) {
        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            int centerIdx = quadIdx * 3;

            // Compute distance using projection of vector from camera->quad center onto shared normal, flipped by sign
            // to accommodate backwards-facing quads in the same plane extensions

            float qX = centers[centerIdx + 0] - x;
            float qY = centers[centerIdx + 1] - y;
            float qZ = centers[centerIdx + 2] - z;

            distanceArray[quadIdx] = (normX * qX + normY * qY + normZ * qZ) * (normalSigns.get(quadIdx) ? 1 : -1);
        }
    }

    private static void buildDynamicDistanceArray(float[] centers, float[] distanceArray, int quadCount, float x,
                                                  float y, float z) {
        // Sort using distance to camera directly
        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            int centerIdx = quadIdx * 3;

            float qX = centers[centerIdx + 0] - x;
            float qY = centers[centerIdx + 1] - y;
            float qZ = centers[centerIdx + 2] - z;
            distanceArray[quadIdx] = qX * qX + qY * qY + qZ * qZ;
        }
    }

    @Override
    public void generateSortedIndexBuffer(ByteBuffer indexBuffer, int quadCount, @Nullable TranslucentQuadAnalyzer.SortState chunkData, float x, float y, float z) {
        if (chunkData == null || chunkData.level() == TranslucentQuadAnalyzer.Level.NONE || chunkData.centersLength() < 3) {
            generateSimpleIndexBuffer(indexBuffer, quadCount);
            return;
        }

        if (quadCount != (chunkData.centersLength() / 3)) {
            throw new IllegalStateException(String.format("Mismatched SortState (%d) vs given quad count (%d)", chunkData.centersLength() / 3, quadCount));
        }

        float[] centers = chunkData.centers();
        int[] indicesArray = new int[quadCount];
        float[] distanceArray = new float[quadCount];
        boolean isStatic = chunkData.level() == TranslucentQuadAnalyzer.Level.STATIC;
        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            indicesArray[quadIdx] = quadIdx;
        }

        if (isStatic) {
            buildStaticDistanceArray(centers, distanceArray,
                    centers[0] + chunkData.sharedNormal().x * FAKE_STATIC_CAMERA_OFFSET,
                    centers[1] + chunkData.sharedNormal().y * FAKE_STATIC_CAMERA_OFFSET,
                    centers[2] + chunkData.sharedNormal().z * FAKE_STATIC_CAMERA_OFFSET,
                    chunkData.sharedNormal().x,
                    chunkData.sharedNormal().y,
                    chunkData.sharedNormal().z,
                    quadCount,
                    chunkData.normalSigns());
        } else {
            buildDynamicDistanceArray(centers, distanceArray, quadCount, x, y, z);
        }

        MergeSort.mergeSort(indicesArray, distanceArray);

        generateIndexBuffer(indexBuffer, indicesArray);
    }
}
