package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.brokers.semp.SempException;
import com.solace.psg.queueBrowser.PaginatedCachingBrowser;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;

public class BrowserDialog {
	private Broker broker;
	private PaginatedCachingBrowser browser;
	private String queue;
	private JFrame parentFrame;
	private int nCurPage = 0;

	private static final int nItemsPerPage = 11;
	private static final int nIdColumn = 1;
	
	private int estimatedPageCount = 0;

	private JLabel topLabel;
	private JTextArea textArea;
	private JTable table;
	private JButton nextMsgButton;
	private JButton delButton;
	private JButton prevMsgButton;
	JDialog dialog; 
	private Semaphore semaphore = new Semaphore(1);
	private int selectedRow;
	private IconicTableCellRenderer iconCellRenderer;
	private ImageIcon messageIcon;
	
	public BrowserDialog(Broker b, String queue, JFrame frame, int nEstimatedMessageCount) throws SempException {
		this.queue = queue;
		this.parentFrame = frame;
		this.broker = b;
		this.estimatedPageCount = (nEstimatedMessageCount / nItemsPerPage) + 1;
		this.iconCellRenderer = new IconicTableCellRenderer();
		this.messageIcon = new ImageIcon("config/messageIcon32.png");
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
		DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);

		// Create the table with the table model
		table = new JTable(tableModel);
		table.setRowHeight(33);

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
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				onSelectMessage(table, row);
			}
		});

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
				onDeleteMessage(table, dialog, tableModel);
			}
		});

		nextMsgButton = new JButton("Next Message >");
		nextMsgButton.setEnabled(false);
		nextMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onNextMessage(table);
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
		
		JPanel buttonLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonLeftPanel.add(prevMsgButton);
		buttonLeftPanel.add(nextMsgButton);
		buttonLeftPanel.add(delButton);

		JPanel buttonRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonRightPanel.add(backButton);
		buttonRightPanel.add(nextButton);

		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(buttonLeftPanel, BorderLayout.WEST);
		buttonPanel.add(buttonRightPanel, BorderLayout.EAST);

		// buttonPanel.add(new JButton("Button 2"));
		topPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Create the bottom panel
		JPanel bottomPanel = new JPanel(new BorderLayout());

		// Add a label at the top of the bottom panel
		JLabel bottomLabel = new JLabel("Bottom Panel Label");
		bottomPanel.add(bottomLabel, BorderLayout.NORTH);

		// Add a large text area to the bottom panel
		bottomPanel.add(textAreaScrollPane, BorderLayout.CENTER);

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

	private void onDeleteMessage(JTable table, Component dialog, DefaultTableModel model) {
		String id = getMessageIdOfSelectedARow();
		
		int response = JOptionPane.showConfirmDialog(dialog, 
                "Are you sure you want to delete message (" + id + ")?", 
                "Confirmation", 
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
    		this.browser.delete(id);
    		int selectedRow = table.getSelectedRow();
    		if (selectedRow != -1) {
    			model.removeRow(selectedRow);
    		}
        } 
	}

	private void onNextMessage(JTable table) {
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

	static class AlternatingRowColorRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				if (row % 2 == 0) {
					c.setBackground(Color.LIGHT_GRAY);
				} else {
					c.setBackground(Color.WHITE);
				}
			} else {
				c.setBackground(table.getSelectionBackground());
			}
			return c;
		}
	}
}