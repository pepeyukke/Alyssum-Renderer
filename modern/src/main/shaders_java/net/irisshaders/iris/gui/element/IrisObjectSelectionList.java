package net.irisshaders.iris.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class IrisObjectSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractSelectionList<E> {
	public IrisObjectSelectionList(Minecraft client, int width, int height, int top, int bottom, int left, int right, int itemHeight) {
        //? if <1.20.4 {
		super(client, width, height, top, bottom, itemHeight);

		this.x0 = left;
		this.x1 = right;
        //?} else {
        /*super(client, width, height, top, itemHeight);
        *///?}
	}

	@Override
	protected int getScrollbarPosition() {
		// Position the scrollbar at the rightmost edge of the screen.
		// By default, the scrollbar is positioned moderately offset from the center.
		return width - 6;
	}

	public void select(int entry) {
		setSelected(this.getEntry(entry));
	}

	@Override
	public void /*? if <1.20.4 {*/ updateNarration /*?} else {*/ /*updateWidgetNarration *//*?}*/(NarrationElementOutput p0) {

	}
}
