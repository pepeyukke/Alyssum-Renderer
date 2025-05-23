package net.irisshaders.iris.texture.mipmap;

import org.embeddedt.embeddium.api.util.ColorABGR;

public class ChannelMipmapGenerator extends AbstractMipmapGenerator {
	protected final BlendFunction redFunc;
	protected final BlendFunction greenFunc;
	protected final BlendFunction blueFunc;
	protected final BlendFunction alphaFunc;

	public ChannelMipmapGenerator(BlendFunction redFunc, BlendFunction greenFunc, BlendFunction blueFunc, BlendFunction alphaFunc) {
		this.redFunc = redFunc;
		this.greenFunc = greenFunc;
		this.blueFunc = blueFunc;
		this.alphaFunc = alphaFunc;
	}

	@Override
	public int blend(int c0, int c1, int c2, int c3) {
		return ColorABGR.pack(
            redFunc.blend(
                ColorABGR.unpackRed(c0),
                ColorABGR.unpackRed(c1),
                ColorABGR.unpackRed(c2),
                ColorABGR.unpackRed(c3)
            ),
            greenFunc.blend(
                ColorABGR.unpackGreen(c0),
                ColorABGR.unpackGreen(c1),
                ColorABGR.unpackGreen(c2),
                ColorABGR.unpackGreen(c3)
            ),
			blueFunc.blend(
                ColorABGR.unpackBlue(c0),
                ColorABGR.unpackBlue(c1),
                ColorABGR.unpackBlue(c2),
                ColorABGR.unpackBlue(c3)
			),
            alphaFunc.blend(
                ColorABGR.unpackAlpha(c0),
                ColorABGR.unpackAlpha(c1),
                ColorABGR.unpackAlpha(c2),
                ColorABGR.unpackAlpha(c3)
            )
		);
	}

	public interface BlendFunction {
		int blend(int v0, int v1, int v2, int v3);
	}
}
