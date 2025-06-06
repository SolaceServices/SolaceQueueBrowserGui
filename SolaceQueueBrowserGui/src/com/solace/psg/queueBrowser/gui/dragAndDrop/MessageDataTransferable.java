package com.solace.psg.queueBrowser.gui.dragAndDrop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class MessageDataTransferable implements Transferable {
    static final DataFlavor FLAVOR = new DataFlavor(DroppableMessage.class, "DroppableMessage");
    private final DroppableMessage data;

    public MessageDataTransferable(DroppableMessage data) {
        this.data = data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(FLAVOR);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!flavor.equals(FLAVOR)) throw new UnsupportedFlavorException(flavor);
        return data;
    }
}