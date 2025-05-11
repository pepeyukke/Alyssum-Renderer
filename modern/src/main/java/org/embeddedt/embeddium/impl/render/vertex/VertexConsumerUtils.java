package org.embeddedt.embeddium.impl.render.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;

public class VertexConsumerUtils {
    /**
     * Attempt to convert a {@link VertexConsumer} into a {@link VertexBufferWriter}. If this fails, return null
     * and log a message.
     * @param consumer the consumer to convert
     * @return a {@link VertexBufferWriter}, or null if the consumer does not support this
     */
    public static VertexBufferWriter convertOrLog(VertexConsumer consumer) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);

        return writer;
    }
}
