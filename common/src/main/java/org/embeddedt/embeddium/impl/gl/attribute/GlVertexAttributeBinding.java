package org.embeddedt.embeddium.impl.gl.attribute;

public class GlVertexAttributeBinding extends GlVertexAttribute {
    private final int index;

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.getName(), attribute.isNormalized(), attribute.getPointer(), attribute.getStride(), attribute.isIntType());

        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
