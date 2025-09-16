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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import com.solace.psg.util.CommandLog;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.ReplicationGroupMessageId;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;

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

	private JLabel topLabel;
	private JLabel filterLabel;
	private JTextArea textArea;
	//private JTextArea propsArea;
	private JTable table;
	private DefaultTableModel tableModel; 
	private JTable headersTable;
	private DefaultTableModel headersTableModel; 
	private JTable propsTable;
	private DefaultTableModel propsTableModel; 
	private JButton nextButton;
	private JButton nextMsgButton;
	private JButton delButton;
	private JButton prevMsgButton;
	private JButton moveMessageMsgButton;
	private JButton copyMessageMsgButton;
	private JButton downloadMessageMsgButton;
	private JLabel statusLabel;
	private JComboBox<String> comboBox;
	JDialog dialog; 
	private Semaphore semaphore = new Semaphore(1);
	private int selectedRow;
	private IconicTableCellRenderer iconCellRenderer;
	private ImageIcon messageIcon;
	private SempClient sempV2ActionClient;

	public Point mousePressPoint;
    FilterSpecification spec = new FilterSpecification();
	String lastIdAdded = "";
	int numberOfMessagesOnTheCurrentPage = -1;

	String headerFields[] = {"Destination","Delivery Mode", "Reply-To Destination", "Time-To-Live (TTL)",
			"DMQ Eligible", "Immediate Acknowledgement", "Redelivery Flag",
			"Deliver-To-One", "Class of Service (CoS)", "Eliding Eligible",
			"Message ID", "Correlation ID", "Message Type", "Encoding"}; //, "Compression"

	public BrowserDialog(SempClient sempV2ActionClient, Broker b, String queue, JFrame frame, int nEstimatedMessageCount, String[] otherQueues) throws SempException {
		this.queue = queue;
		this.otherQueues = otherQueues;
		this.parentFrame = frame;
		this.broker = b;
		this.estimatedPageCount = (nEstimatedMessageCount / nItemsPerPage) + 1;
		this.iconCellRenderer = new IconicTableCellRenderer();
		this.messageIcon = new ImageIcon("config/messageIcon32.png");
		this.sempV2ActionClient = sempV2ActionClient;
		
		//spec.bodyValue = "the text you seek";
		
		this.initialize();
	}

	private void initialize() throws SempException {
		this.browser = new PaginatedCachingBrowser(broker, this.queue, nItemsPerPage);
		this.browser.setFilter(spec);
	}

	@SuppressWarnings("serial")
	void run() throws JCSMPException {
		int totalTableWidth = 1480; 
		// Create the dialog
		dialog = new JDialog(parentFrame, "Solace Queue Browser - " + this.queue, true);
		dialog.setSize(1600, 1200);
		dialog.setLayout(new BorderLayout());
		dialog.setModal(false);

		// Create the top panel
		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel topTextMessages = new JPanel(new BorderLayout());
		topLabel = new JLabel("Message in the " + this.queue + " queue. Showing page " + nCurPage + " of about "
				+ estimatedPageCount);
		filterLabel = new JLabel("<html><br>Filter: '" + spec.bodyValue + "'; showing only messages where this text is contained in the payload<br><br></html>");


		topTextMessages.add(topLabel, BorderLayout.NORTH);
//		topTextMessages.add(filterButton, BorderLayout.CENTER);
		topTextMessages.add(filterLabel, BorderLayout.SOUTH);
		
		
		topPanel.add(topTextMessages, BorderLayout.NORTH);
		
		String[][] data = new String[][] {};
		String[] columnNames = { "", "Message Id", "Size", "Redelivered?" };

		// Create the table model
		tableModel = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Disable editing for all cells
			}
		};

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

		nextButton = new JButton("Next Page >>");
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

        downloadMessageMsgButton = new JButton("Download");
        downloadMessageMsgButton.setEnabled(false);
        downloadMessageMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onDownloadMessage();
			}
		});

		JButton filterButton = new JButton("Filter messages...");
		filterButton.setEnabled(true);
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onClickFilter(dialog, tableModel, filterButton);
			}
		});

		JPanel buttonLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonLeftPanel.add(prevMsgButton);
		buttonLeftPanel.add(nextMsgButton);
		buttonLeftPanel.add(delButton);
		buttonLeftPanel.add(filterButton);

		JPanel buttonMiddlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonMiddlePanel.add(moveMessageMsgButton);
		buttonMiddlePanel.add(copyMessageMsgButton);
		buttonMiddlePanel.add(comboBox);
		buttonMiddlePanel.add(downloadMessageMsgButton);
		
		
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
		//JLabel bottomLabel = new JLabel("Payload");
		//bottomPanel.add(bottomLabel, BorderLayout.NORTH);

		// Add a large text area to the bottom panel
		JPanel payloadPanel = new JPanel();
		payloadPanel.setLayout(new BoxLayout(payloadPanel, BoxLayout.Y_AXIS));

		payloadPanel.add(new JLabel("Payload"));
		textArea = new JTextArea(10, 40);
		payloadPanel.add(textArea);
		JScrollPane textAreaScrollPane = new JScrollPane(payloadPanel);

		String[][] headerData = new String[][] {};
		String[] headerColumnNames = {"Property", "Value" };
		headersTableModel = new DefaultTableModel(headerData, headerColumnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Disable editing for all cells
			}
		};
		// Create the table with the table model
		headersTable = new JTable(headersTableModel);
		headersTable.setRowHeight(24);

		String[][] propsData = new String[][] {};
		String[] propColumnNames = {"Property", "Value" };
		propsTableModel = new DefaultTableModel(propsData, propColumnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Disable editing for all cells
			}
		};
		// Create the table with the table model
		propsTable = new JTable(propsTableModel);
		propsTable.setRowHeight(24);
				
		
		JPanel propsTablesPanel = new JPanel();
		propsTablesPanel.setLayout(new BoxLayout(propsTablesPanel, BoxLayout.Y_AXIS));

		// Add a label at the top of the bottom panel
		propsTablesPanel.add(new JLabel("Headers"));
		propsTablesPanel.add(headersTable);
		propsTablesPanel.add(new JLabel("User Properties"));
		propsTablesPanel.add(propsTable);
		

		JScrollPane propsAreaScrollPane = new JScrollPane(propsTablesPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, propsAreaScrollPane, textAreaScrollPane);
        splitPane.setDividerLocation(350); // Initial divider position
        splitPane.setOneTouchExpandable(true); // Adds little arrows to collapse/expand
		
		bottomPanel.add(splitPane, BorderLayout.CENTER);

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
	
	private void onClickFilter(JDialog dialog, DefaultTableModel tableModel, JButton filterButton) {
		
		FilterDialog filterD = new FilterDialog(dialog, this.spec);
		filterD.run();
		
		if (filterD.cancelled == false) {
	
			try {
				initialize();
			} catch (SempException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	//		dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	 
			SwingUtilities.invokeLater(() -> {
				restartAfterFilter();
			});
		}
		
	}
	private void restartAfterFilter() {
		
		SpinnerDialog spinner = new SpinnerDialog(dialog);
		
		tableModel.setRowCount(0);
		preFetch();
		nCurPage = -1;
		onNextPage(dialog, tableModel, nextButton);
		
		spinner.setVisible(false);
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
		String selectedTargetQueue = (String) comboBox.getSelectedItem();
		String id = getMessageIdOfSelectedARow();
		System.out.println("moving message " + id + " to " + selectedTargetQueue);
		
		BytesXMLMessage msg = browser.get(id);
		ReplicationGroupMessageId replicationId = msg.getReplicationGroupMessageId();
		try {
			sempV2ActionClient.copy(broker.msgVpnName, queue, selectedTargetQueue, replicationId.toString());

			String action = "copied";
			if (deleteFromSource == true) {
				action = "moved";
			}
			String logMsg = "MessageId " + id + " (replication id='" + replicationId.toString() + "') was " + action + 
					" from the '" + this.queue + "' queue to the '" + selectedTargetQueue + "'.";
			CommandLog.instance().log(logMsg);
			
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
	
	private String propsAsString(String[][] props) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < props.length; i++) {
			String[] row = props[i];
			String name = row[0];
			String value = row[1];
			sb.append(name + ": " + value + "\n");
		}
		return sb.toString();
	}
	
	private void makeDirIfRequired(String path) throws IOException {
		File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs(); // Creates directory and any necessary parent dirs
            if (!created) {
            	throw new IOException("Failed to create directory '" + path + "'.");
            }
        }
	}
	private void writeStringToFile(String fileName, String payload) throws IOException {
        @SuppressWarnings("resource")
		FileWriter writer = new FileWriter(fileName);
        writer.write(payload);
        writer.close();
	}
	private void deleteFile(String filePath) throws IOException {
	    File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
            	throw new IOException("Failed to deletre file " + filePath);
            }
        }
	}
	private void onDownloadMessage() {
		try {
			String id = getMessageIdOfSelectedARow();
			BytesXMLMessage message = this.browser.get(id);
			String payload = browser.getPayload(message);
			String[][] headers = getMessageHeadersData(message);
			String[][] userProps = getMessageUserPropsData(message);
			
			String folder = "./downloads";
			makeDirIfRequired(folder);
			
			String payloadFile = folder + "/payload.txt"; 
			writeStringToFile(payloadFile, payload);
			
			payload = propsAsString(headers);
			String headersFile = folder + "/headers.txt"; 
			writeStringToFile(headersFile, payload);

			payload = propsAsString(userProps);
			String userPropsFile = folder + "/userProps.txt"; 
			writeStringToFile(userPropsFile, payload);

			//StringBuilder sb = new StringBuilder();
			
			String zipFileName = folder + "/message-" + id + ".zip";

	        FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            addToZip(zipOut, payloadFile);
            addToZip(zipOut, headersFile);
            addToZip(zipOut, userPropsFile);
            zipOut.close();
            fos.close();

            deleteFile(payloadFile);
            deleteFile(headersFile);
            deleteFile(userPropsFile);
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void addToZip(ZipOutputStream zipOut, String srcFile) throws IOException {
        File fileToZip = new File(srcFile);
        FileInputStream fis = new FileInputStream(fileToZip); 
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) >= 0) {
            zipOut.write(buffer, 0, length);
        }
        fis.close();
	}
	private void onDeleteMessage(JTable table, Component dialog) {
		String id = getMessageIdOfSelectedARow();
		
		int response = JOptionPane.showConfirmDialog(dialog, 
                "Are you sure you want to delete message (" + id + ")?", 
                "Confirmation", 
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
        	doDelete(id);
        	
			String logMsg = "MessageId " + id + " was deleted from the '" + this.queue + "' queue.";
			CommandLog.instance().log(logMsg);

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
	
	private String getHeaderValue(BytesXMLMessage message, String field) {
		String rc = "";
		if (field.equals("Destination")) {
			rc = message.getDestination().getName();
		}
		else if (field.equals("Delivery Mode")) {
			rc = message.getDeliveryMode().name();
		}
		else if (field.equals("Reply-To Destination")) {
			Destination dest = message.getReplyTo();
			if (dest != null) {
				rc = dest.getName();
			}
		}
		else if (field.equals("Time-To-Live (TTL)")) {
			rc = "" + message.getTimeToLive();
		}
		else if (field.equals("DMQ Eligible")) {
			rc = "" + message.isDMQEligible();
		}
		else if (field.equals("Immediate Acknowledgement")) {
			rc = "" + message.isAckImmediately();
		}
		else if (field.equals("Redelivery Flag")) {
			rc = "" + message.getRedelivered();
		}
		else if (field.equals("Deliver-To-One")) {
			rc = "" + message.getDeliverToOne();
		}
		else if (field.equals("Class of Service (CoS)")) {
			rc = "" + message.getCos().ordinal();
		}
		else if (field.equals("Eliding Eligible")) {
			rc = "" + message.isElidingEligible();
		}
		else if (field.equals("Message ID")) {
			rc = message.getMessageId();
		}
		else if (field.equals("Correlation ID")) {
			rc = message.getCorrelationId();
		}
		else if (field.equals("Message Type")) {
			rc = message.getMessageType().toString();
		}
		else if (field.equals("Encoding")) {
			rc = message.getHTTPContentEncoding();
		}
		return rc;
	}
	private String[][] getMessageHeadersData(BytesXMLMessage message) {
		String[][] data = new String[headerFields.length][];
		for (int i = 0; i < headerFields.length; i++) {
			String value = "?";
			data[i] = new String[2];
			data[i][0] = headerFields[i];
			data[i][1] = getHeaderValue(message, headerFields[i]);
		}
		return data;
	}
	
	private String[][] getMessageUserPropsData(BytesXMLMessage message) throws SDTException {
		SDTMap map = message.getProperties();
		
		String[][] data = new String[map.size()][];

		Set<String> keys = map.keySet();
		int i = 0;
		for (String key : keys) {
	        Object value = map.get(key);
			data[i] = new String[2];
			data[i][0] = key;
			data[i][1] = value.toString();
			i++;
		}
		return data;
	}
	
	private void onSelectMessage(JTable table, int row) {
		try {
			this.selectedRow = row;
			String id = getMessageIdOfSelectedARow();
			String payload = browser.getPayload(id);
			textArea.setText(payload);
			textArea.setCaretPosition(0);

			boolean moreRowsAvailable = false;
			if (row < (numberOfMessagesOnTheCurrentPage - 1)) {
				moreRowsAvailable = true;
			}
			
			// populate the headers and user proprties
			DefaultTableModel headersTableModel = (DefaultTableModel) headersTable.getModel();
			headersTableModel.setRowCount(0); // Clears all existing rows
			DefaultTableModel propsTableModel = (DefaultTableModel) propsTable.getModel();
			propsTableModel.setRowCount(0); // Clears all existing rows
			
			BytesXMLMessage message = this.browser.get(id);
			Object[][] newHeadersData = getMessageHeadersData(message);// new String[][] {};
			for (Object[] rowData : newHeadersData) {
				headersTableModel.addRow(rowData); // Add new rows
			}
			Object[][] newData = getMessageUserPropsData(message);// new String[][] {};
			for (Object[] rowData : newData) {
				propsTableModel.addRow(rowData); // Add new rows
			}

			
			nextMsgButton.setEnabled(moreRowsAvailable);
			delButton.setEnabled(true);
			prevMsgButton.setEnabled(row > 0);
			
			moveMessageMsgButton.setEnabled(true);
			copyMessageMsgButton.setEnabled(true);
			downloadMessageMsgButton.setEnabled(true);
			
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
		topLabel.setText("Message in the " + this.queue + " queue. Showing page " + nCurPage + " of up to about "
				+ estimatedPageCount);
		textArea.setText("");
	}

	private void display(DefaultTableModel tableModel, Object[][] dataUpdate) {
		tableModel.setRowCount(0);
		numberOfMessagesOnTheCurrentPage = 0;
		for (Object[] oneRow : dataUpdate) {
			tableModel.addRow(oneRow);
			lastIdAdded = (String) oneRow[nIdColumn];
			numberOfMessagesOnTheCurrentPage++;
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
		nextButton.setEnabled(shouldNextPageButtonBeActive());

		onPageChange();
		dialog.setCursor(Cursor.getDefaultCursor());
		SwingUtilities.invokeLater(() -> {
			autoSelectFirstRow();
		});
	}

	private boolean shouldNextPageButtonBeActive() {
		return browser.hasMoreAfterId(lastIdAdded);
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
		
		// see if the browser has any more messages after the last one onscreen
		nextButton.setEnabled(shouldNextPageButtonBeActive());
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
		
		
		String logMsg = "MessageId " + msg.id + " (replication id='" + msg.replicationId.toString() + "') was moved " +  
				" from the '" + msg.queue + "' queue to the '" + msg.targetQueue + "'.";
		CommandLog.instance().log(logMsg);
		
		//onNextMessage(this.table);		
	}
}