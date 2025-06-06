package com.solace.psg.queueBrowser.gui.dragAndDrop;

import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;


@SuppressWarnings("serial")
public class QueueMessageTransferInstigatorHandler extends TransferHandler {
	String title;
	IDragDropInstigator owner;
    public QueueMessageTransferInstigatorHandler(IDragDropInstigator owner, String title) {
		this.title = title;
		this.owner = owner;
	}

	@Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTable table = (JTable) c;
        int row = table.getSelectedRow();
        
        DroppableMessage msg = owner.getMessageBeingDragged(row);
        MessageDataTransferable xfer = new MessageDataTransferable(msg);
        //Icon icon = (Icon) table.getValueAt(row, 0);
        return xfer;
    }

    @Override
    public boolean canImport(TransferSupport support) {
    	return false;
    }

    @Override
    public boolean importData(TransferSupport support) {
        return false;
    }
}
