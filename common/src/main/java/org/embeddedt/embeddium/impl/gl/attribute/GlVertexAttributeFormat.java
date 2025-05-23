package org.embeddedt.embeddium.impl.gl.attribute;

import org.lwjgl.opengl.GL20C;

/**
 * An enumeration over the supported data types that can be used for vertex attributes.
 */
public record GlVertexAttributeFormat(int typeId, int size) {
    public static final GlVertexAttributeFormat FLOAT = new GlVertexAttributeFormat(GL20C.GL_FLOAT, 4);
    public static final GlVertexAttributeFormat SHORT = new GlVertexAttributeFormat(GL20C.GL_SHORT, 2);
    public static final GlVertexAttributeFormat UNSIGNED_SHORT = new GlVertexAttributeFormat(GL20C.GL_UNSIGNED_SHORT, 2);
    public static final GlVertexAttributeFormat BYTE = new GlVertexAttributeFormat(GL20C.GL_BYTE, 1);
    public static final GlVertexAttributeFormat UNSIGNED_BYTE = new GlVertexAttributeFormat(GL20C.GL_UNSIGNED_BYTE, 1);
    public static final GlVertexAttributeFormat UNSIGNED_INT = new GlVertexAttributeFormat(GL20C.GL_UNSIGNED_INT, 4);
}
