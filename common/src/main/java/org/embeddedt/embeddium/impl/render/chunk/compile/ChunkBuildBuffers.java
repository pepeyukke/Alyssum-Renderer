package org.embeddedt.embeddium.impl.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.BakedChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public final class ChunkBuildBuffers {
    private static final ModelQuadFacing[] ONLY_UNASSIGNED = new ModelQuadFacing[] { ModelQuadFacing.UNASSIGNED };
    private final Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders = new Reference2ReferenceOpenHashMap<>();

    @Getter
    private final RenderPassConfiguration<?> renderPassConfiguration;

    private BuiltRenderSectionData renderData;
    private int sectionIndex;

    public ChunkBuildBuffers(RenderPassConfiguration<?> configuration) {
        this.renderPassConfiguration = configuration;
    }

    public void init(BuiltRenderSectionData renderData, int sectionIndex) {
        this.renderData = renderData;
        this.sectionIndex = sectionIndex;
        for (var builder : this.builders.values()) {
            builder.begin(renderData, sectionIndex);
        }
    }

    public BuiltRenderSectionData getSectionContextBundle() {
        return this.renderData;
    }

    public ChunkModelBuilder get(Material material) {
        return this.get(material.pass);
    }

    private ChunkModelBuilder createBuilder(TerrainRenderPass pass) {
        var vertexType = this.renderPassConfiguration.getVertexTypeForPass(pass);
        var builder = new BakedChunkModelBuilder(vertexType.createEncoder(), vertexType.getVertexFormat().getStride(), pass);
        Objects.requireNonNull(renderData, "Buffers have not been started");
        builder.begin(renderData, sectionIndex);
        this.builders.put(pass, builder);
        return builder;
    }

    public ChunkModelBuilder get(TerrainRenderPass pass) {
        var builder = this.builders.get(pass);
        return builder != null ? builder : createBuilder(pass);
    }

    public Set<TerrainRenderPass> getBuilderPasses() {
        return this.builders.keySet();
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public BuiltSectionMeshParts createMesh(TerrainRenderPass pass, float camX, float camY, float camZ) {
        var builder = this.builders.get(pass);

        if (builder == null || builder.isEmpty()) {
            return null;
        }

        List<ByteBuffer> vertexBuffers = new ArrayList<>();
        VertexRange[] vertexRanges = new VertexRange[ModelQuadFacing.COUNT];

        int vertexCount = 0;

        ModelQuadFacing[] facingsToUpload = pass.isSorted() ? ONLY_UNASSIGNED : ModelQuadFacing.VALUES;
        TranslucentQuadAnalyzer.SortState sortState = pass.isSorted() ? builder.getVertexBuffer(ModelQuadFacing.UNASSIGNED).getSortState() : null;

        for (ModelQuadFacing facing : facingsToUpload) {
            var buffer = builder.getVertexBuffer(facing);

            if (buffer.isEmpty()) {
                continue;
            }

            vertexBuffers.add(buffer.slice());
            vertexRanges[facing.ordinal()] = new VertexRange(vertexCount, buffer.count());

            vertexCount += buffer.count();
        }

        if (vertexCount == 0) {
            return null;
        }

        var vertexType = renderPassConfiguration.getVertexTypeForPass(pass);
        var primitiveType = renderPassConfiguration.getPrimitiveTypeForPass(pass);

        var mergedBuffer = new NativeBuffer(vertexCount * vertexType.getVertexFormat().getStride());
        var mergedBufferBuilder = mergedBuffer.getDirectBuffer();

        for (var buffer : vertexBuffers) {
            mergedBufferBuilder.put(buffer);
        }

        mergedBufferBuilder.flip();

        NativeBuffer mergedIndexBuffer;

        if (pass.isSorted()) {
            int numPrimitives = vertexCount / primitiveType.getVerticesPerPrimitive();
            // Generate the canonical index buffer
            mergedIndexBuffer = new NativeBuffer(primitiveType.getIndexBufferSize(numPrimitives));

            // Do the initial sort now
            primitiveType.generateSortedIndexBuffer(mergedIndexBuffer.getDirectBuffer(), numPrimitives, sortState, camX, camY, camZ);
        } else {
            mergedIndexBuffer = null;
        }

        return new BuiltSectionMeshParts(mergedBuffer, mergedIndexBuffer, TranslucentQuadAnalyzer.SortState.compacted(sortState), vertexRanges);
    }

    public void destroy() {
        this.sectionIndex = 0;
        this.renderData = null;
        for (var builder : this.builders.values()) {
            builder.destroy();
        }
    }
}
