package net.irisshaders.batchedentityrendering.impl.wrappers;

import net.minecraft.client.renderer.RenderType;

import java.util.function.Function;

public interface WrappingMultiBufferSource {
	void pushWrappingFunction(Function<RenderType, RenderType> wrappingFunction);

	void popWrappingFunction();

	void assertWrapStackEmpty();
}
