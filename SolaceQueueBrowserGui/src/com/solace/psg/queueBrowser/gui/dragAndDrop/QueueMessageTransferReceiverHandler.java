package com.solace.psg.queueBrowser.gui.dragAndDrop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;


@SuppressWarnings("serial")
public class QueueMessageTransferReceiverHandler extends TransferHandler {
	String title;
	IDragDropTarget owner;
    public QueueMessageTransferReceiverHandler(IDragDropTarget owner, String title) {
		this.title = title;
		this.owner = owner;
	}

	@Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
    	throw new RuntimeException("cannot initiate a drag in a recipient");
    }

    @Override
    public boolean canImport(TransferSupport support) {
    	return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
        int row = dropLocation.getRow();

        try {
        	Transferable xferAble = support.getTransferable();
        	System.out.println("dropping " + xferAble.getClass().getTypeName());
        	DataFlavor[] flavors = xferAble.getTransferDataFlavors();
        	System.out.println("falvs = " + flavors.toString() ); 
        	
        	for (DataFlavor oneF : flavors) {
        		if (oneF.equals(MessageDataTransferable.FLAVOR)) {
                    System.out.println("here");
        		}
        	}
        	DroppableMessage msg = (DroppableMessage) xferAble.getTransferData(MessageDataTransferable.FLAVOR);
            System.out.println("dropped on " + row + " from queue " + msg.queue);
            owner.onDrop(row, msg);
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }

        return false;
    }
}
