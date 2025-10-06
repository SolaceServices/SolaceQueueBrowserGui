package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.brokers.semp.SempClient;
import com.solace.psg.brokers.semp.SempClient.QueueInfo;
import com.solace.psg.brokers.semp.SempClient.eApi;
import com.solace.psg.brokers.semp.SempException;
import com.solace.psg.queueBrowser.gui.QueueActionWindow.eAction;
import com.solace.psg.queueBrowser.gui.dragAndDrop.DroppableMessage;
import com.solace.psg.queueBrowser.gui.dragAndDrop.IDragDropTarget;
import com.solace.psg.queueBrowser.gui.dragAndDrop.QueueMessageTransferReceiverHandler;
import com.solacesystems.jcsmp.JCSMPException;

public class QueueBrowserMainWindow implements IDragDropTarget {
	private static final Logger logger = LoggerFactory.getLogger(QueueBrowserMainWindow.class.getName());

	private SempClient sempV2ConfigClient;
	private SempClient sempV2MonitorClient;
	private SempClient sempV2ActionClient;;
	Broker broker;
	List<String> queues;
	String selectedQueue = "";
	int selectedQueueMsgCount = 0;
	String configFile;
	Config thisCfg = null;
	private JFrame frame;
	private JLabel detailsLabel;
	private JPanel buttonPanel;
	private JButton moveAllButton;
	private JButton refreshButton;
	private JButton browseButton;
	private JButton copyAllButton;
	private JButton deleteAllButton;

	private IconicTableCellRenderer iconCellRenderer;
	JLabel qIconlabel; 

	private DefaultTableModel tableModel;

	private JTable table;


	class ListItem {
	    private String text;
	    private Icon icon;
	    public ListItem(String text, Icon icon) {
	        this.text = text;
	        this.icon = icon;
	    }
	    public String getText() {
	        return text;
	    }
	    public Icon getIcon() {
	        return icon;
	    }
	    public String toString() {
	        return text;
	    }
	}
	
	public QueueBrowserMainWindow(String configFile) throws BrokerException {
		this.configFile = configFile;
		this.initialize();
	}

	private void initialize() throws BrokerException {
		thisCfg = new Config(this.configFile);
		thisCfg.load();
		broker = thisCfg.broker;
		sempV2ConfigClient = SempClient.SempClientFactory(eApi.eConfig, broker.sempHost, broker.sempAdminUser,
				broker.sempAdminPw);
		sempV2ActionClient = SempClient.SempClientFactory(eApi.eAction, broker.sempHost, broker.sempAdminUser,
				broker.sempAdminPw);
		sempV2MonitorClient = SempClient.SempClientFactory(eApi.eMonitor, broker.sempHost, broker.sempAdminUser,
				broker.sempAdminPw);
		queues = sempV2ConfigClient.getAllQueueNames(broker.msgVpnName);
		
		Collections.sort(queues);
		
		this.iconCellRenderer = new IconicTableCellRenderer();
	}

	private Object[][] getTableData(List<String> queueNames) {
		ImageIcon qIcon = new ImageIcon("config/queueSm.png");
		Object[][] data = new Object[queueNames.size()][];
		for (int i = 0; i < queueNames.size(); i++) {
			//ArrayList<String> row = queueNames.get(i);
			String q = queueNames.get(i);
			data[i] = new Object[2];
			data[i][0] = qIcon;
			data[i][1] = q;
		}
		return data;

	}

	private void run() {
			// Create the frame
			frame = new JFrame("Solace Queue Maintenace Tool");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(1200, 800);
			frame.setLayout(new BorderLayout());

			ImageIcon icon = new ImageIcon("config/queueBrowserIcon.png");
			Image image = icon.getImage();
			frame.setIconImage(image);

			Object[][] data = getTableData(queues);// new String[][] {};
			String[] columnNames = { "", "Queue Name"};

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
			table.setFillsViewportHeight(true);
			table.setDefaultRenderer(Object.class, new AlternatingRowColorRenderer());
			table.getColumnModel().getColumn(0).setCellRenderer(iconCellRenderer);
			table.getColumnModel().getColumn(0).setMaxWidth(48);
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int row = table.rowAtPoint(e.getPoint());
					onSelectQueue(table, row);
				}
				@Override
			    public void mousePressed(MouseEvent e) {
			        if (e.getClickCount() == 2) {
			            int row = table.rowAtPoint(e.getPoint());
			            System.out.println("Double-clicked row: " + row);
						try {
							onBrowse(selectedQueue, frame);
						} catch (JCSMPException | SempException e1 ) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
			        }
			    }

			});
	        table.setTransferHandler(new QueueMessageTransferReceiverHandler(this, "target-Q"));
	        table.getActionMap().put("upArrow", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					int row = table.getSelectedRow();
					if (row > 0) {
						row--;
						table.setRowSelectionInterval(row, row);
						onSelectQueue(table, row);
					}
				}
			});

			// DOWN arrow key binding
			table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DOWN"),"downArrow");
			table.getActionMap().put("downArrow", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					int row = table.getSelectedRow();
					int rowCount = table.getRowCount();

					if (row < (rowCount - 1)) {
						row++;
						table.setRowSelectionInterval(row, row);
						onSelectQueue(table, row);
					}
				}
			});
			
			JPanel listPanel = new JPanel(new BorderLayout());
			listPanel.setPreferredSize(new Dimension(400, listPanel.getPreferredSize().height)); // Set the preferred width
			listPanel.add(new JLabel("Queues"), BorderLayout.NORTH);
			
			JScrollPane scrollingList = new JScrollPane(table);
			scrollingList.setBorder(new EmptyBorder(4, 4, 4, 4)); // Top, Left, Bottom, Right
			listPanel.add(scrollingList, BorderLayout.CENTER);

			detailsLabel = new JLabel();// TextArea();
			detailsLabel.setFont(new Font("Serif", Font.ITALIC, 16));
			
			detailsLabel.setText("<html>"
	                + "<div style='width: 280px; text-align: left; vertical-align:top'>"
	                + "<p><i>Select a queue on the left to see details.</i></p>"
	                + "</div>"
	                + "</html>");
			detailsLabel.setVerticalAlignment(SwingConstants.TOP);
	        
	        JPanel detailsPanel = new JPanel();
	        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
			
	        qIconlabel = new JLabel(new ImageIcon("config/queue.png"));
	        qIconlabel.setVisible(false);
	        
	        //JPanel iconTopPanel = new JPanel(new BorderLayout());
	        //iconTopPanel.add(qIconlabel, BorderLayout.CENTER);

	        detailsPanel.add(qIconlabel);
	        detailsPanel.add(detailsLabel);
	        
			JScrollPane textScrollPane = new JScrollPane(detailsPanel);
			textScrollPane.setBorder(new EmptyBorder(4, 4, 4, 4)); // Top, Left, Bottom, Right

			buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			browseButton = new JButton("Browse");
			browseButton.setEnabled(false);
			browseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						onBrowse(selectedQueue, frame);
					} catch (SempException | JCSMPException e1) {
						e1.printStackTrace();
					}
				}
			});

			copyAllButton = new JButton("Copy all...");
			copyAllButton.setEnabled(false);
			copyAllButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onCopyAll(selectedQueue, frame);
				}
			});

			moveAllButton = new JButton("Move all...");
			moveAllButton.setEnabled(false);
			moveAllButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onMoveAll(selectedQueue, frame);
				}
			});

			deleteAllButton = new JButton("Delete all");
			deleteAllButton.setEnabled(false);
			deleteAllButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onDeleteAll(selectedQueue, frame);
				}

			});

			refreshButton = new JButton("Refresh");
			refreshButton.setEnabled(true);
			refreshButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onRefresh();
				}
			});

			this.addButtons(buttonPanel);

			JLabel iconLabel = new JLabel("");
			iconLabel.setIcon(icon);

			JLabel greetingLine0 = new JLabel("Browsing: " + broker.name);
			greetingLine0.setBorder(new EmptyBorder(0, 0, 6, 0)); // Top, Left, Bottom, Right
			greetingLine0.setFont(new Font("Arial", Font.BOLD, 18));

			JLabel greetingLine1 = new JLabel("      FQDN: " + broker.fqdn());
			greetingLine1.setBorder(new EmptyBorder(0, 0, 6, 0)); // Top, Left, Bottom, Right
			greetingLine1.setFont(greetingLine0.getFont());

			JLabel greetingLine2 = new JLabel("         VPN: " + broker.msgVpnName);
			greetingLine2.setFont(greetingLine0.getFont());

			JPanel wordsPanel = new JPanel();
			wordsPanel.setLayout(new BoxLayout(wordsPanel, BoxLayout.Y_AXIS));
			wordsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); // Add some margin
			wordsPanel.add(greetingLine0);
			wordsPanel.add(greetingLine1);
			wordsPanel.add(greetingLine2);

			JPanel topPanel = new JPanel(new BorderLayout());
			topPanel.add(iconLabel,BorderLayout.WEST);
			topPanel.add(wordsPanel,BorderLayout.CENTER);

			JPanel rightPanel = new JPanel();
			rightPanel.setLayout(new BorderLayout());
			rightPanel.add(new JLabel("Queue details"), BorderLayout.NORTH);
			rightPanel.add(textScrollPane, BorderLayout.CENTER);

			frame.add(topPanel, BorderLayout.NORTH);
			frame.add(listPanel, BorderLayout.WEST);
			frame.add(rightPanel, BorderLayout.CENTER);
			frame.add(buttonPanel, BorderLayout.SOUTH);

			frame.setVisible(true);
	}
	private void onRefresh() {
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		SwingUtilities.invokeLater(() -> {

			DefaultTableModel model = (DefaultTableModel) table.getModel();
			model.setRowCount(0); // Clears all existing rows

			try {
				queues = sempV2ConfigClient.getAllQueueNames(broker.msgVpnName);
				Collections.sort(queues);

				Object[][] newData = getTableData(queues);// new String[][] {};
				for (Object[] rowData : newData) {
				    model.addRow(rowData); // Add new rows
				}

			} catch (SempException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			frame.setCursor(Cursor.getDefaultCursor());
		});
	}
	
	@Override
	public void onDrop(int row, DroppableMessage msg) {
		String qDroppedOn = (String) table.getValueAt(row, 1);
		System.out.println("Dropped on " + qDroppedOn);
		
		try {
			sempV2ActionClient.copy(broker.msgVpnName, msg.queue, qDroppedOn, msg.replicationId);
			System.out.println("Message copied to " + qDroppedOn);
			
			// test the delete
			msg.targetQueue = qDroppedOn;
			msg.source.onMessageWasMoved(msg);
			
		} catch (SempException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void onSelectQueue(JTable table, int row) {
		selectedQueue = getSelectedQueue();
		browseButton.setEnabled(true);
		copyAllButton.setEnabled(true);
		deleteAllButton.setEnabled(true);
		moveAllButton.setEnabled(true);
		
		try {
			this.onQueueNameSelected(selectedQueue, detailsLabel, buttonPanel);
		} catch (SempException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getSelectedQueue() {
//		ListItem item = (ListItem) listBox.getSelectedValue();
//		return item.text.trim();
		return(String) table.getValueAt(table.getSelectedRow(), 1);
	}
	private void addButtons(JPanel buttonPanel) {
		buttonPanel.add(refreshButton);
		buttonPanel.add(copyAllButton);
		buttonPanel.add(moveAllButton);
		buttonPanel.add(deleteAllButton);
		buttonPanel.add(browseButton);
	}

	private void onBrowse(String queueName, JFrame frame) throws SempException, JCSMPException {
		String[] otherQueues = getListOfQueuesExceptCurrentlySelectedOne(queueName);
		BrowserDialog d = new BrowserDialog(this.sempV2ActionClient, this.broker, queueName, frame,
				selectedQueueMsgCount, otherQueues, thisCfg.downloadFolder);
		d.run();
	}

	private String[] getListOfQueuesExceptCurrentlySelectedOne(String selectedQueue) {
		List<String> tempList = new ArrayList<String>();
		for (String oneStr : queues) {
			if (oneStr.equals(selectedQueue) == false) {
				tempList.add(oneStr);	
			}
		}
		String[] options = (String[]) tempList.toArray(new String[0]);
		return options;
	}

	private String pickAQueue(JFrame frame) {
		QueueSelectorDialog dlg = new QueueSelectorDialog();
		String[] options = this.getListOfQueuesExceptCurrentlySelectedOne(selectedQueue);
		String destQueue = dlg.selectQueues(frame, options);
		return destQueue;
	}
	private void onCopyAll(String selectedQueue, JFrame frame) {
		String destQueue = pickAQueue(frame);
		QueueActionWindow cp;
		try {
			cp = new QueueActionWindow(frame, broker, eAction.eCopy, sempV2ActionClient, sempV2MonitorClient,
					broker.msgVpnName, selectedQueue, destQueue);
			cp.run();
		} catch (BrokerException e) {
			e.printStackTrace();
		}
	}

	private void onMoveAll(String selectedQueue, JFrame frame) {
		QueueActionWindow cp;
		try {
			String destQueue = pickAQueue(frame);

			cp = new QueueActionWindow(frame, broker, eAction.eMove, sempV2ActionClient, sempV2MonitorClient,
					broker.msgVpnName, selectedQueue, destQueue);
			cp.run();

			SwingUtilities.invokeLater(() -> {
				try {
					System.out.println("REFRESH");
					onQueueNameSelected(selectedQueue, detailsLabel, buttonPanel);
				} catch (SempException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (BrokerException e) {
			e.printStackTrace();
		}
	}

	private void onDeleteAll(String selectedQueue, JFrame frame) {
		int response = JOptionPane.showConfirmDialog(frame,
				"Are you sure you want to delete all messages in the '" + selectedQueue + "' queue? IMPORTANT: This action cannot be undone.", "Confirmation",
				JOptionPane.YES_NO_OPTION);

		if (response == JOptionPane.YES_OPTION) {
			QueueActionWindow cp;
			try {
				cp = new QueueActionWindow(frame, broker, eAction.eDelete, sempV2ActionClient, sempV2MonitorClient,
						broker.msgVpnName, selectedQueue, "");
				cp.run();

				SwingUtilities.invokeLater(() -> {
					try {
						System.out.println("REFRESH");
						onQueueNameSelected(selectedQueue, detailsLabel, buttonPanel);
					} catch (SempException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			} catch (BrokerException e) {
				e.printStackTrace();
			}
		}
	}
	 public static String splitCamelCase(String str) {
	        // Use a regular expression to split camelCase
	        String[] words = str.split("(?=[A-Z])"); // Matches positions before uppercase letters

	        // Capitalize the first word
	        if (words.length > 0) {
	            words[0] = words[0].substring(0, 1).toUpperCase() + words[0].substring(1);
	        }

	        // Join the words with spaces
	        return String.join(" ", words);
	    }


	private void addRowToDisplay(QueueInfo info, StringBuilder sb, String fieldName, boolean big) {
         try {
             Field field = info.getClass().getDeclaredField(fieldName);
             field.setAccessible(true); // Allow access to private fields
             Object value = field.get(info);
             String strValue = "---";
             if (value != null) {
            	 strValue = value.toString();
            	 if (strValue.isEmpty()) {
            		 strValue = "---";
            	 }
             }
             
             if (fieldName.equals("partitionCount")) {
            	 int val = Integer.parseInt(value.toString());
            	 if (val > 0) {
            		 strValue += " - CANNOT browse"; 
            	 }
             }
             
             String style = " ";
             if (big) {
            	 style = " style='font-size: 20px;' ";
             }
             
             String fieldNameForDisply = splitCamelCase(fieldName);
             sb.append("<tr><td width='180px' align='right'>" + fieldNameForDisply + ":</td><td" + style + " align='left'>" + strValue + "</td></tr>");
             
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		} 
         
	}
	private void onQueueNameSelected(String queueName, JLabel textArea2, JPanel buttonPanel)
			throws SempException, IOException {
		
		QueueInfo info  = sempV2MonitorClient.getQueueInfo(broker.msgVpnName, queueName);
		selectedQueueMsgCount = info.msgCount;

		//String display = "Queue name: " + queueName + "\nCurrent Message Count: " + selectedQueueMsgCount;
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<div style='width: 580px; text-align: left; vertical-align:top'>");

		sb.append("<div style='width: 580px; text-align: left; vertical-align:top'>");
		sb.append("<table>");
		addRowToDisplay(info, sb, "name", true);
		addRowToDisplay(info, sb, "msgCount", false);
		addRowToDisplay(info, sb, "accessType", false);
		addRowToDisplay(info, sb, "maxMsgSpoolUsage", false);
		addRowToDisplay(info, sb, "owner", false);
		addRowToDisplay(info, sb, "permission", false);
		addRowToDisplay(info, sb, "egressEnabled", false);
		addRowToDisplay(info, sb, "ingressEnabled", false);
		addRowToDisplay(info, sb, "partitionCount", false);

        sb.append("</table>");
        sb.append("</div>");
        sb.append("</html>");
		
        String display = sb.toString();
        
//		String display = "<html>"
//	        + "<div style='width: 280px; text-align: left; vertical-align:top'>"
//	        + "<table>"
//	        + "  <tr><th align='right'>Name:</th><th align='left'><b>{queueName}</th>"
//	        + "  <tr><th align='right'>Message Count:</th><th align='left'><b>{msgCount}</th>"
//	        + "  <tr><th align='right'>Access Type:</th><th align='left'><b>{accessType}</th>"
//	        + "  <tr><th align='right'>Owner:</th><th align='left'><b>{owner}</th>"
//	        + "  <tr><th align='right'>Permission:</th><th align='left'><b>{permission}</th>"
//	        + "  <tr><th align='right'>Max Spool:</th><th align='left'><b>{maxMsgSpoolUsage}</th>"
//	        + "</table>"
//	        + "</div>"
//	        + "</html>";
//		
//		display = display.replace("{queueName}", "" + info.name);
//		display = display.replace("{msgCount}", "" + info.msgCount);
//		display = display.replace("{accessType}", "" + info.accessType);
//		display = display.replace("{maxMsgSpoolUsage}", "" + info.maxMsgSpoolUsage);
//		display = display.replace("{owner}", "" + info.owner);
//		display = display.replace("{permission}", "" + info.permission);

		textArea2.setText(display);
		qIconlabel.setVisible(true);
		
		buttonPanel.removeAll();
		this.addButtons(buttonPanel);

//        // any spec on where this can move to?
//        List<String> destinations = getMovementDestinationsFor(queueName);
//        if (destinations.size() > 0) {
//        	for (String oneDest: destinations) {
//                JButton moveAllButton = new JButton("Move all to " + oneDest);
//                moveAllButton.setEnabled(true);
//                MoveActionListener listener = new MoveActionListener(queueName, oneDest);
//                moveAllButton.addActionListener(listener);
//                buttonPanel.add(moveAllButton);
//        	}
//            buttonPanel.revalidate();
//            buttonPanel.repaint();
//        }
	}
//	class MoveActionListener implements ActionListener {
//		
//		private String source;
//		private String target;
//
//		public MoveActionListener(String source, String target) {
//			this.source = source;
//			this.target = target;
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			// TODO Auto-generated method stub
//			System.out.println("click " + source + " -> " + target);
//		}
//	}
//	

//	private List<String> getMovementDestinationsFor(String strQueue) {
//		List<String> rc = new ArrayList<String>();
//		
//		for (Map.Entry<String, MovementDestination> entry : thisCfg.movements.entrySet()) {
//            String key = entry.getKey();
//            MovementDestination value = entry.getValue();
//            
//            if (value.source.equals(strQueue) || value.source.equals("all")) {
//            	for (String oneDest : value.destinations) {
//                	rc.add(oneDest);
//            	}
//            }
//		}
//		return rc;
//	}

	public static void main(String[] args) throws BrokerException {
		CommandLineParser parser = new CommandLineParser();
		parser.parseArgs(args);
		logger.info("Configuration File: " + parser.configFileProvided);

		QueueBrowserMainWindow me = new QueueBrowserMainWindow(parser.configFileProvided);
		me.run();
	}
}