package org.embeddedt.embeddium.impl.buffer;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public interface ExtendedVertexFormat {
    Element[] embeddium$getExtendedElements();

    class Element {
        public final VertexFormatElement actual;
        public final int increment;
        public final int byteLength;

        public Element(VertexFormatElement actual, int increment, int byteLength) {
            this.actual = actual;
            this.increment = increment;
            this.byteLength = byteLength;
        }
    }
}