package com.solace.psg.queueBrowser.gui.dragAndDrop;

public interface IDragDropInstigator {
	DroppableMessage getMessageBeingDragged(int row);
	void onMessageWasMoved(DroppableMessage msg);
}
