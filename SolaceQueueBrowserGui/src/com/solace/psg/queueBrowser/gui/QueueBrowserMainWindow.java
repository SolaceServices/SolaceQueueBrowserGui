package com.solace.psg.queueBrowser.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.brokers.semp.SempClient;
import com.solace.psg.brokers.semp.SempClient.ePaginationBehavior;
import com.solace.psg.brokers.semp.SempException;
import com.solacesystems.jcsmp.JCSMPException;

public class QueueBrowserMainWindow {
	private static final Logger logger = LoggerFactory.getLogger(QueueBrowserMainWindow.class.getName());

	private SempClient sempV2ConfigClient;
	private SempClient sempV2MonitorClient;
	Broker broker;
	List<String> queues;
	String selectedQueue = "";
	int selectedQueueMsgCount = 0;
	String configFile;
	Config thisCfg = null;

	private JButton browseButton;;
	public QueueBrowserMainWindow(String configFile) throws BrokerException {
		this.configFile = configFile;
		this.initialize();
	}
	private void initialize() throws BrokerException {
		thisCfg = new Config(this.configFile);
		thisCfg.load();
		broker = thisCfg.broker;
        sempV2ConfigClient = new SempClient(broker.sempHost, broker.sempAdminUser, broker.sempAdminPw);
        
        String monitorEndPointUrl = broker.sempHost.replace("config", "monitor");
        sempV2MonitorClient = new SempClient(monitorEndPointUrl, broker.sempAdminUser, broker.sempAdminPw);
        queues = sempV2ConfigClient.getAllQueueNames(broker.msgVpnName);
        
        /*
        if (thisCfg.blackListedQueues != null) {
        	queues = blacklist(queues, thisCfg.blackListedQueues);
        }
        else if (thisCfg.whiteListedQueues != null) {
        	queues = whitelist(queues, thisCfg.whiteListedQueues);
        }
        */
	}

	/* This feature set has been deferred from the current release.
	 * 
	private List<String> whitelist(List<String> queues, List<String> whiteListedQueues) {
		List<String> queuesRc = new ArrayList<String>();
		for (String queue : queues) {
			if (whiteListedQueues.contains(queue)) {
				queuesRc.add(queue);
				logger.info("queue '" + queue + "' is whitelisted, and will be included in the GUI");
			}
		}
		return queuesRc;
	}
	private List<String> blacklist(List<String> queues, List<String> blackListedQueues) {
		List<String> queuesRc = new ArrayList<String>();
		for (String queue : queues) {
			if (blackListedQueues.contains(queue) == false) {
				queuesRc.add(queue);
			}
			else {
				logger.info("queue '" + queue + "' is blacklisted, and will be excluded in the GUI");
			}
		}
		return queuesRc;
	}
	*/
	private void run() {
        SwingUtilities.invokeLater(() -> {
            // Create the frame
            JFrame frame = new JFrame("Solace Queue Maintenace Tool");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 800);
            frame.setLayout(new BorderLayout());
            //frame.setLayout(null);
            
            ImageIcon icon = new ImageIcon("config/queueBrowserIcon.png");
            Image image = icon.getImage();
            frame.setIconImage(image);

            // Create a list of items
            //String[] items = {"Item 1", "Item 2", "Item 3", "Item 4"};
            String[] items = queues.toArray(new String[0]);
            
            // Create the JList and add it to a scroll pane
            JList<String> listBox = new JList<>(items);
            //JScrollPane listScrollPane = new JScrollPane(listBox);
            //listScrollPane.setSize(400, 800);

            
            //JPanel image
//            try {
//				BufferedImage image2 = ImageIO.read(new File("config/queueBrowserIcon.png"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

            JPanel listPanel = new JPanel(new BorderLayout());
            listPanel.setPreferredSize(new Dimension(400, listPanel.getPreferredSize().height)); // Set the preferred width
            listPanel.add(new JLabel("Queues"), BorderLayout.NORTH);
            listPanel.add(new JScrollPane(listBox), BorderLayout.CENTER);
            
            // Create the text area
            JTextArea textArea = new JTextArea();
            textArea.setFont(new Font("Serif", Font.ITALIC, 16));
            textArea.setText("--select a queue on the left to see details--");
            
            textArea.setEditable(false);
            JScrollPane textScrollPane = new JScrollPane(textArea);

            // Create a panel for the buttons
            JPanel buttonPanel = new JPanel();
            //buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            browseButton = new JButton("Browse");
            browseButton.setEnabled(false);
            browseButton.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	            	try {
						onBrowse(selectedQueue, frame);
					} catch (SempException | JCSMPException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	            }
	        });
            this.addBrowseButton(buttonPanel);
//            buttonPanel.add(browseButton);

            // Add action listener to the list
            listBox.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    textArea.setFont(new Font("Serif", Font.PLAIN, 16));
                    selectedQueue = listBox.getSelectedValue();
                    browseButton.setEnabled(true);
                    try {
						onQueueNameSelected(selectedQueue, textArea, buttonPanel);
					} catch (SempException | IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                }
            });
            listBox.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Get the selected item
                        //int index = listBox.locationToIndex(e.getPoint());
                        //String selectedItem = listBox.getModel().getElementAt(index);
                        // Handle the double-click event
                        textArea.setFont(new Font("Serif", Font.PLAIN, 16));
                        selectedQueue = listBox.getSelectedValue();
                        browseButton.setEnabled(true);
                        try {
    						onQueueNameSelected(selectedQueue, textArea, buttonPanel);
    					} catch (SempException | IOException e1) {
    						// TODO Auto-generated catch block
    						e1.printStackTrace();
    					}
                        try {
							onBrowse(selectedQueue, frame);
						} catch (SempException | JCSMPException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                    }
                }
            });
            
            JLabel label =  new JLabel("");
            label.setIcon(icon);
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(label);



            // Create a panel for the right side
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BorderLayout());
            rightPanel.add(new JLabel("Queue details"), BorderLayout.NORTH);
            rightPanel.add(textScrollPane, BorderLayout.CENTER);

            // Add components to the frame
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(listPanel, BorderLayout.WEST);
            frame.add(rightPanel, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            // Make the frame visible
            frame.setVisible(true);
        });
    }
	private void addBrowseButton(JPanel buttonPanel) {
        buttonPanel.add(browseButton);
	}
	private void onBrowse(String queueName, JFrame frame) throws SempException, JCSMPException {
		BrowserDialog d = new BrowserDialog(this.broker, queueName, frame, selectedQueueMsgCount);
		d.run();
		
//		try {
//			this.sempV2MonitorClient.test(this.broker.msgVpnName, queueName);
//		} catch (SempException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private void onQueueNameSelected(String queueName, JTextArea textArea, JPanel buttonPanel) throws SempException, IOException {
		String resource = "/msgVpns/" + broker.msgVpnName + "/queues/" + queueName;
		String result = sempV2MonitorClient.getSempV2(resource, ePaginationBehavior.eNone);
		
		JSONObject doc = new JSONObject(result);
		//JSONObject data = doc.getJSONObject("data");
		JSONObject collections = doc.getJSONObject("collections");
		JSONObject msgs = collections.getJSONObject("msgs");
		selectedQueueMsgCount = msgs.getInt("count");
		
		String display = "Queue name: " + queueName + "\nCurrent Message Count: " + selectedQueueMsgCount; 
        textArea.setText(display);
        
        buttonPanel.removeAll();
        this.addBrowseButton(buttonPanel);
        
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