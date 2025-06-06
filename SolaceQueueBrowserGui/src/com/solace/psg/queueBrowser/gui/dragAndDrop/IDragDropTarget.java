package com.solace.psg.queueBrowser.gui.dragAndDrop;

public interface IDragDropTarget {
	void onDrop(int row, DroppableMessage msg);
}
