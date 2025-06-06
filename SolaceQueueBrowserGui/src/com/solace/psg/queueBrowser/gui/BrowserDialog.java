package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableModel;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.brokers.semp.SempClient;
import com.solace.psg.brokers.semp.SempException;
import com.solace.psg.queueBrowser.PaginatedCachingBrowser;
import com.solace.psg.queueBrowser.gui.dragAndDrop.DroppableMessage;
import com.solace.psg.queueBrowser.gui.dragAndDrop.IDragDropInstigator;
import com.solace.psg.queueBrowser.gui.dragAndDrop.QueueMessageTransferInstigatorHandler;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.ReplicationGroupMessageId;

public class BrowserDialog implements IDragDropInstigator {
	private Broker broker;
	private PaginatedCachingBrowser browser;
	private String queue;
	private String[] otherQueues;
	private JFrame parentFrame;
	private int nCurPage = 0;

	private static final int nItemsPerPage = 11;
	private static final int nIdColumn = 1;
	
	private int estimatedPageCount = 0;

	private DefaultTableModel tableModel; 
	private JLabel topLabel;
	private JTextArea textArea;
	private JTable table;
	private JButton nextMsgButton;
	private JButton delButton;
	private JButton prevMsgButton;
	private JButton moveMessageMsgButton;
	private JButton copyMessageMsgButton;
	private JLabel statusLabel;
	private JComboBox<String> comboBox;
	JDialog dialog; 
	private Semaphore semaphore = new Semaphore(1);
	private int selectedRow;
	private IconicTableCellRenderer iconCellRenderer;
	private ImageIcon messageIcon;
	private SempClient sempV2ActionClient;

	public Point mousePressPoint;

	public BrowserDialog(SempClient sempV2ActionClient, Broker b, String queue, JFrame frame, int nEstimatedMessageCount, String[] otherQueues) throws SempException {
		this.queue = queue;
		this.otherQueues = otherQueues;
		this.parentFrame = frame;
		this.broker = b;
		this.estimatedPageCount = (nEstimatedMessageCount / nItemsPerPage) + 1;
		this.iconCellRenderer = new IconicTableCellRenderer();
		this.messageIcon = new ImageIcon("config/messageIcon32.png");
		this.sempV2ActionClient = sempV2ActionClient;
		this.initialize();
	}

	private void initialize() throws SempException {
		this.browser = new PaginatedCachingBrowser(broker, this.queue, nItemsPerPage);
	}

	void run() throws JCSMPException {
		int totalTableWidth = 1480; 
		// Create the dialog
		dialog = new JDialog(parentFrame, "Solace Queue Browser - " + this.queue, true);
		dialog.setSize(totalTableWidth, 1000);
		dialog.setLayout(new BorderLayout());
		dialog.setModal(false);

		// this text area gos in the bottom pane, but need to define it up here so the
		// mouse handler on thetable click sees it
		textArea = new JTextArea(10, 40);
		JScrollPane textAreaScrollPane = new JScrollPane(textArea);

		// Create the top panel
		JPanel topPanel = new JPanel(new BorderLayout());

		// Add a label at the top of the top panel
		topLabel = new JLabel("Message in the " + this.queue + " queue. Showing page " + nCurPage + " of about "
				+ estimatedPageCount);
		topPanel.add(topLabel, BorderLayout.NORTH);

		String[][] data = new String[][] {};
		String[] columnNames = { "", "Message Id", "Size", "Redelivered?" };

		// Create the table model
		tableModel = new DefaultTableModel(data, columnNames);

		// Create the table with the table model
		table = new JTable(tableModel);
		table.setRowHeight(33);
        table.setDragEnabled(true);

		// Set a custom cell renderer to alternate row colors
		table.setDefaultRenderer(Object.class, new AlternatingRowColorRenderer());
		table.getColumnModel().getColumn(0).setCellRenderer(iconCellRenderer);
		
		table.getColumnModel().getColumn(0).setPreferredWidth(36);
		int remainindWidth = totalTableWidth - 36;
        table.getColumnModel().getColumn(1).setPreferredWidth(remainindWidth/3);
        table.getColumnModel().getColumn(2).setPreferredWidth(remainindWidth/3);
        table.getColumnModel().getColumn(3).setPreferredWidth(remainindWidth/3);
        
		// Enable gridlines
		table.setShowGrid(true);
		table.setGridColor(Color.BLACK);
//		table.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				int row = table.rowAtPoint(e.getPoint());
//				onSelectMessage(table, row);
//			}
//		});
        table.addMouseListener(new TableMouseListener(table, this));
        table.addMouseMotionListener(new TableMouseMotionListener(table));
        table.setTransferHandler(new QueueMessageTransferInstigatorHandler(this, "source"));

		JScrollPane listScrollPane = new JScrollPane(table);
		listScrollPane.setPreferredSize(new Dimension(380, 400));
		topPanel.add(listScrollPane, BorderLayout.CENTER);

		JButton backButton = new JButton("<< Previous Page");
		backButton.setEnabled(false);
		backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onPreviousPage(dialog, tableModel, backButton);
			}
		});

		JButton nextButton = new JButton("Next Page >>");
		nextButton.setEnabled(false);
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onNextPage(dialog, tableModel, backButton);
			}
		});

		delButton = new JButton("Delete");
		delButton.setEnabled(false);
		delButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onDeleteMessage(table, dialog);
			}
		});

		nextMsgButton = new JButton("Next Message >");
		nextMsgButton.setEnabled(false);
		nextMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onNextMessage();
			}
		});
		prevMsgButton = new JButton("< Previous Message");
		prevMsgButton.setEnabled(false);
		prevMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onPreviousMessage(table);
			}
		});
		copyMessageMsgButton = new JButton("Copy to Queue:");
		copyMessageMsgButton.setEnabled(false);
		copyMessageMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onCopyMessage();
			}
		});

		moveMessageMsgButton = new JButton("Move to Queue:");
		moveMessageMsgButton.setEnabled(false);
		moveMessageMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onMoveMessage();
			}
		});
        comboBox = new JComboBox<>(otherQueues);
        Dimension preferredSize = comboBox.getPreferredSize();
        preferredSize.width = 400;
        comboBox.setPreferredSize(preferredSize);

		
		JPanel buttonLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonLeftPanel.add(prevMsgButton);
		buttonLeftPanel.add(nextMsgButton);
		buttonLeftPanel.add(delButton);

		JPanel buttonMiddlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonMiddlePanel.add(moveMessageMsgButton);
		buttonMiddlePanel.add(copyMessageMsgButton);
		buttonMiddlePanel.add(comboBox);
		
		JPanel buttonRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonRightPanel.add(backButton);
		buttonRightPanel.add(nextButton);

		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(buttonLeftPanel, BorderLayout.WEST);
		buttonPanel.add(buttonMiddlePanel, BorderLayout.CENTER);
		buttonPanel.add(buttonRightPanel, BorderLayout.EAST);

		// buttonPanel.add(new JButton("Button 2"));
		topPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Create the bottom panel
		JPanel bottomPanel = new JPanel(new BorderLayout());

		// Add a label at the top of the bottom panel
		JLabel bottomLabel = new JLabel("Payload");
		bottomPanel.add(bottomLabel, BorderLayout.NORTH);

		// Add a large text area to the bottom panel
		bottomPanel.add(textAreaScrollPane, BorderLayout.CENTER);

		statusLabel = new JLabel("Browsing " + this.queue);
		statusLabel.setFont(new Font("Arial", Font.PLAIN, 22));
		bottomPanel.add(statusLabel, BorderLayout.SOUTH);

		
		// Add the top and bottom panels to the dialog
		dialog.add(topPanel, BorderLayout.NORTH);
		dialog.add(bottomPanel, BorderLayout.CENTER);

		// Center the dialog on the screen
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setLocation(parentFrame.getLocation().x + 10, parentFrame.getLocation().y + 10);

		// Make the dialog visible
		// dialog.setVisible(true);

		SwingUtilities.invokeLater(() -> {
			// JOptionPane.showMessageDialog(dialog, "later");
			dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			preFetch();
			nCurPage = -1;
			onNextPage(dialog, tableModel, nextButton);
		});

		dialog.setVisible(true);
	}
	

	private void autoSelectFirstRow() {
		table.setRowSelectionInterval(0, 0);
		onSelectMessage(table, 0);
	}
	
	private void onMoveMessage() {
		moveOrCopy(true);
	}
	private void onCopyMessage() {
		moveOrCopy(false);
	}
	private void moveOrCopy(boolean deleteFromSource) {
		String selectedOption = (String) comboBox.getSelectedItem();
		String id = getMessageIdOfSelectedARow();
		System.out.println("moving message " + id + " to " + selectedOption);
		
		BytesXMLMessage msg = browser.get(id);
		ReplicationGroupMessageId replicationId = msg.getReplicationGroupMessageId();
		try {
			sempV2ActionClient.copy(broker.msgVpnName, queue, selectedOption, replicationId.toString());
		} catch (SempException e1) {
			e1.printStackTrace();
		}
		if (deleteFromSource) {
			browser.delete(id);
			int selectedRow = table.getSelectedRow();
			if (selectedRow != -1) {
				tableModel.removeRow(selectedRow);
			}
		}
	}
	
	private void onDeleteMessage(JTable table, Component dialog) {
		String id = getMessageIdOfSelectedARow();
		
		int response = JOptionPane.showConfirmDialog(dialog, 
                "Are you sure you want to delete message (" + id + ")?", 
                "Confirmation", 
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
        	doDelete(id);
        } 
	}
	private void doDelete(String id) {
		this.browser.delete(id);
		int selectedRow = table.getSelectedRow();
		
		// skp ahead first so that the onSelect event handling properly sahows the next message
		onNextMessage();
		
		// now axe the row that was deleted
		if (selectedRow != -1) {
			tableModel.removeRow(selectedRow);
		}
		
	}

	private void onNextMessage() {
		int nRow = this.selectedRow + 1;
		table.setRowSelectionInterval(nRow, nRow);
		onSelectMessage(table, nRow);
	}
	private void onPreviousMessage(JTable table) {
		int nRow = this.selectedRow - 1;
		table.setRowSelectionInterval(nRow, nRow);
		onSelectMessage(table, nRow);
	}

	private String getMessageIdOfSelectedARow() {
		return(String) table.getValueAt(this.selectedRow, nIdColumn);
	}
	
	private void onSelectMessage(JTable table, int row) {
		try {
			this.selectedRow = row;
			String id = getMessageIdOfSelectedARow();
			String payload = browser.getPayload(id);
			textArea.setText(payload);
			textArea.setCaretPosition(0);

			boolean moreRowsAvailable = false;
			if (row < (nItemsPerPage - 1)) {
				moreRowsAvailable = true;
			}
			nextMsgButton.setEnabled(moreRowsAvailable);
			delButton.setEnabled(true);
			prevMsgButton.setEnabled(row > 0);
			
			moveMessageMsgButton.setEnabled(true);
			copyMessageMsgButton.setEnabled(true);
			
			setStatus("Viewing message " + id);
		} catch (Throwable t) {
			System.out.println(t.getLocalizedMessage());
		}
	}

	private void preFetch() {
		try {
			semaphore.acquire();
			browser.prefetchNextPage();
		} catch (BrokerException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			semaphore.release();
		}
	}

	private void onPageChange() {
		topLabel.setText("Message in the " + this.queue + " queue. Showing page " + nCurPage + " of about "
				+ estimatedPageCount);
		textArea.setText("");
	}

	private void display(DefaultTableModel tableModel, Object[][] dataUpdate) {
		tableModel.setRowCount(0);
		for (Object[] oneRow : dataUpdate) {
			tableModel.addRow(oneRow);
		}
		if (dataUpdate.length > 0) {
			autoSelectFirstRow();
		}
	}

	private void onPreviousPage(JDialog dialog, DefaultTableModel tableModel, JButton backButton) {
		dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		nCurPage--;
		
		Object[][] dataUpdate = null;
		try {
			dataUpdate = this.getMessages();
		} catch (BrokerException e) {
			e.printStackTrace();
		} 
		display(tableModel, dataUpdate);

		if (nCurPage < 1) {
			backButton.setEnabled(false);
		}
		onPageChange();
		dialog.setCursor(Cursor.getDefaultCursor());
		SwingUtilities.invokeLater(() -> {
			autoSelectFirstRow();
		});
	}

	int rowCount = 0;
	private void onNextPage(JDialog dialog, DefaultTableModel tableModel, JButton backButton) {
		dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		nCurPage++;
		Object[][] dataUpdate = null;
		
		try {
			dataUpdate = this.getMessages();
			rowCount = dataUpdate.length;
		} catch (BrokerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		display(tableModel, dataUpdate);

		backButton.setEnabled(true);
		onPageChange();
		dialog.setCursor(Cursor.getDefaultCursor());

		SwingUtilities.invokeLater(() -> {
			
			if (rowCount > 0) {
				autoSelectFirstRow();
			}
			preFetch();
		});
	}

	private Object[][] getMessages() throws BrokerException {
		// Create an ArrayList of ArrayList<String> to store the data
		ArrayList<ArrayList<String>> dynamicArray = new ArrayList<>();
		ArrayList<BytesXMLMessage> thisPage = browser.getPage(nCurPage);

		for (BytesXMLMessage message : thisPage) {
			ArrayList<String> row = new ArrayList<>();
			@SuppressWarnings("deprecation")
			String id = message.getMessageId();
			row.add(id);

			int size = message.getAttachmentContentLength();
			if (size == 0) {
				size = message.getBinaryMetadataContentLength(size);
			}
			row.add("" + size);

			String yN = "No";
			if (message.getRedelivered()) {
				yN = "Yes";
			}
			row.add(yN);

			//			String payload = null;
			//			if (message instanceof TextMessage) {
			//				TextMessage txt = (TextMessage) message;
			//				payload = txt.getText();
			//			} else {
			//				byte[] b = message.getBytes();
			//				payload = new String(b);
			//			}
			dynamicArray.add(row);
		}

		// Convert the ArrayList to a 2D array
		Object[][] data = new Object[dynamicArray.size()][];
		for (int i = 0; i < dynamicArray.size(); i++) {
			ArrayList<String> row = dynamicArray.get(i);
			data[i] = new Object[row.size() + 1];
			data[i][0] = messageIcon;
			for (int y = 0; y < row.size(); y++) {
				data[i][y+1] = row.get(y);	
			}
		}
		return data;
	}
	
    private class TableMouseListener extends MouseAdapter {
        private final JTable table;
		BrowserDialog browserDialog;
        public TableMouseListener(JTable table, BrowserDialog browserDialog) {
            this.table = table;
            this.browserDialog = browserDialog; 
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mousePressPoint = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mousePressPoint = null;
            // Handle selection logic here if needed
            handleSelection(e);
        }

        private void handleSelection(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint());
            table.setRowSelectionInterval(row, row);
            
//			int row = table.rowAtPoint(e.getPoint());
            
    		SwingUtilities.invokeLater(() -> {
    	           browserDialog.onSelectMessage(table, row);
    		});

        }
    }
    private class TableMouseMotionListener extends MouseMotionAdapter {
        private final JTable table;

        public TableMouseMotionListener(JTable table) {
            this.table = table;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mousePressPoint != null) {
                Point dragPoint = e.getPoint();
                int dragDistance = (int) mousePressPoint.distance(dragPoint);
                if (dragDistance > 5) { // Threshold distance to start drag
                    TransferHandler handler = table.getTransferHandler();
                    handler.exportAsDrag(table, e, TransferHandler.MOVE);
                }
            }
        }
    }

	@Override
	public DroppableMessage getMessageBeingDragged(int row) {
		String id = (String) table.getValueAt(row, nIdColumn);
		BytesXMLMessage msg = browser.get(id);
		ReplicationGroupMessageId replicationId = msg.getReplicationGroupMessageId();
		
		DroppableMessage dmsg = new DroppableMessage();
		dmsg.id = id;
		dmsg.queue = this.queue;
		dmsg.replicationId = replicationId.toString();
		dmsg.source = this;
		return dmsg;
	}

	private void setStatus(String txt) {
		SwingUtilities.invokeLater(() -> {
			statusLabel.setText(txt);
		});
	}
	@Override
	public void onMessageWasMoved(DroppableMessage msg) {
		doDelete(msg.id);
		setStatus("Message " + msg.id + " was moved from " + msg.queue + " to " + msg.targetQueue);
		//onNextMessage(this.table);		
	}
}