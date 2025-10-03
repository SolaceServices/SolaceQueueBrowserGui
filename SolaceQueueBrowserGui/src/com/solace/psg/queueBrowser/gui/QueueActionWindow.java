package com.solace.psg.queueBrowser.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.brokers.semp.SempClient;
import com.solace.psg.brokers.semp.SempClient.QueueInfo;
import com.solace.psg.brokers.semp.SempException;
import com.solace.psg.queueBrowser.QueueBrowser;
import com.solace.psg.util.CommandLog;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ReplicationGroupMessageId;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QueueActionWindow extends JPanel {
	private static final long serialVersionUID = 1L;

	// Modern color scheme (consistent with other components)
	private static final Color PRIMARY_COLOR = new Color(0x2196F3);
	private static final Color SUCCESS_COLOR = new Color(0x4CAF50);
	private static final Color WARNING_COLOR = new Color(0xFF9800);
	private static final Color ERROR_COLOR = new Color(0xF44336);
	private static final Color SURFACE_COLOR = new Color(0xFAFAFA);

	public enum eAction {eCopy, eMove, eDelete};
	private eAction eActionSelected;
	private int topY = 10;
	private int flyingX = 150; // Starting X position of the flying object
	private int flyingY = topY + 30; // Y position of the flying object
	private int shovelX = 70; // Starting X position for the shovel
	private int shovelY = topY + 10; // Y position for the shovel
	private int shovelDX = 1; // Shovel's horizontal movement speed
	private final ImageIcon flyingIcon;
	private ImageIcon shovelIcon;
	private ImageIcon srcQIcon;
	private ImageIcon tarQIcon;
	private String srcQName;
	private Timer shovelTimer;
	private Timer progressTimer;
	private SwingWorker<Void, Integer> worker;
	private SempClient sempV2ActionClient;
	private SempClient sempV2MonitorClient;
	private int totalMsgCount;
	private int msgsProcessed;
	private String msgVpnName;
	private JFrame parentFrame;
	private JLabel progressLabel;
	private QueueBrowser solaceBrowserObject;
	private Broker broker;
	private String windowTitle;
	private String srcQlabelTitle;
	private String tarQName;
	
	public QueueActionWindow(JFrame parentFrame, Broker broker, eAction action, SempClient sempV2ActionClient, SempClient sempV2MonitorClient, 
			String msgVpnName, String queueName, String destQnameName ) throws BrokerException {
		this.sempV2ActionClient = sempV2ActionClient;
		this.sempV2MonitorClient = sempV2MonitorClient;
		this.srcQName = queueName;
		this.tarQName = destQnameName;
		this.msgVpnName = msgVpnName;
		this.parentFrame = parentFrame;
		this.broker = broker;
		this.eActionSelected = action;
		
		if (action == eAction.eCopy) {
			windowTitle = "Copy All Messages";
			srcQlabelTitle = "Copying all messages from:";
		}
		else if (action == eAction.eMove) {
			windowTitle = "Move All Messages";
			srcQlabelTitle = "Moving all messages from:";
		}
		else if (action == eAction.eDelete) {
			windowTitle = "Delete All Messages";
			srcQlabelTitle = "Deleting all messages from:";
		}
		
		if ((action == eAction.eCopy) || (action == eAction.eMove)) {
			if ((destQnameName == null) || (destQnameName.isEmpty())) {
				throw new BrokerException("Must suppy a destQueue name to copy or move messages");
			}
		}
		
		flyingIcon = new ImageIcon("config/messageIcon32.png");
		shovelIcon = new ImageIcon("config/shovelSm.png");

		srcQIcon = new ImageIcon("config/queue.png");
		
		if (action == eAction.eDelete) {
			tarQIcon = new ImageIcon("config/trash.png");
		}
		else {
			tarQIcon = new ImageIcon("config/queue.png");
		}

		shovelTimer = new Timer(30, e -> {
			shovelX += shovelDX;
			if (shovelX < 70 || shovelX > 100) {
				shovelDX = -shovelDX;
			}

			flyingX += 3; // Constant speed
			if (flyingX > 500) {
				flyingX = 130; // Reset to start left of the screen
			}
			repaint(); // Update the screen
		});
		shovelTimer.start();
	}

	private JButton createStyledButton(String text, Color backgroundColor) {
		JButton button = new JButton(text);
		button.setBackground(backgroundColor);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setFont(new Font("Segoe UI", Font.BOLD, 12));
		button.setPreferredSize(new Dimension(button.getPreferredSize().width + 16, 32));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return button;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		srcQIcon.paintIcon(this, g, 10, topY + 10);
		tarQIcon.paintIcon(this, g, 500, topY + 10);
		shovelIcon.paintIcon(this, g, shovelX, shovelY);
		flyingIcon.paintIcon(this, g, flyingX, flyingY);
	}

	public void run() {
		try {
			QueueInfo info = sempV2MonitorClient.getQueueInfo(msgVpnName, srcQName);
			totalMsgCount = info.msgCount;
		} catch (SempException e1) {
			e1.printStackTrace();
		}
		msgsProcessed = 0;
		solaceBrowserObject = new QueueBrowser(broker, this.srcQName, 50);

		JDialog frame = new JDialog (parentFrame, windowTitle, true);
		frame.setLocationRelativeTo(parentFrame);
		frame.setLocation(parentFrame.getLocation().x + 10, parentFrame.getLocation().y + 10);
		frame.setIconImage(parentFrame.getIconImage());

		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS)); // Set BoxLayout on content pane

		JPanel verticalPanel = new JPanel();
		verticalPanel.setLayout(new BoxLayout(verticalPanel, BoxLayout.Y_AXIS));
		verticalPanel.setBackground(SURFACE_COLOR);
		verticalPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

		JLabel labelTop = new JLabel(srcQlabelTitle);
		labelTop.setFont(new Font("Segoe UI", Font.BOLD, 16));
		labelTop.setForeground(PRIMARY_COLOR);
		verticalPanel.add(labelTop);

		JLabel labelLine2 = new JLabel("Source queue: '" + srcQName + "'.");
		labelLine2.setBorder(new EmptyBorder(8, 24, 0, 0));
		labelLine2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		verticalPanel.add(labelLine2);

		if ((eActionSelected == eAction.eCopy) || (eActionSelected == eAction.eMove)) {
			JLabel labelLine3 = new JLabel("Target queue: '" + tarQName + "'.");
			labelLine3.setBorder(new EmptyBorder(8, 24, 16, 0));
			labelLine3.setFont(new Font("Segoe UI", Font.PLAIN, 14));
			verticalPanel.add(labelLine3);
		}

		JProgressBar progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setStringPainted(true);
		progressBar.setBorder(new EmptyBorder(0, 10, 0, 10)); // Top, Left, Bottom, Right

		JPanel parentPanel = new JPanel();
		parentPanel.setLayout(new BorderLayout());
		parentPanel.setBackground(SURFACE_COLOR);
		parentPanel.add(this, BorderLayout.CENTER);
		parentPanel.add(verticalPanel, BorderLayout.SOUTH);

		frame.add(parentPanel); // , BorderLayout.CENTER);
		
		progressLabel = new JLabel(this.getProgressLabelText());
		progressLabel.setBorder(new EmptyBorder(8, 8, 8, 8));
		progressLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BorderLayout());
		labelPanel.setBackground(SURFACE_COLOR);
		labelPanel.add(progressLabel, BorderLayout.WEST);

		frame.add(labelPanel);// , BorderLayout.SOUTH);
		frame.add(progressBar);// , BorderLayout.SOUTH);

		JButton cancelButton = createStyledButton("Cancel", ERROR_COLOR);
		cancelButton.setEnabled(true);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					worker.cancel(true);
					shovelTimer.stop();
					progressTimer.stop();
					frame.dispose();
				});
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
		buttonPanel.setBackground(SURFACE_COLOR);
		buttonPanel.setBorder(new EmptyBorder(1, 4, 1, 4)); // Top, Left, Bottom, Right
		buttonPanel.add(cancelButton);
		frame.add(buttonPanel);

		frame.setSize(600, 380);
		frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		progressTimer = new Timer(100, e -> {
			// Simulate progress bar updates
			int currentValue = progressBar.getValue();
			if (currentValue < progressBar.getMaximum()) {
				progressBar.setValue(currentValue + 1);
			} else {
				progressBar.setValue(0); // Reset progress bar once it completes
			}
		});
		//progressTimer.start();

		// Background task using SwingWorker
		worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i <= totalMsgCount; i++) {
                    if (isCancelled()) {
                        break; // Exit task if canceled
                    }
                	try {
						if (solaceBrowserObject.hasNext()) {
							BytesXMLMessage msg = solaceBrowserObject.next();
			    			
							boolean axeIt = false;
							if (eActionSelected != eAction.eDelete) {
								// copy or move.. 
								ReplicationGroupMessageId replicationId = msg.getReplicationGroupMessageId();
				    			sempV2ActionClient.copy(msgVpnName, srcQName, tarQName, replicationId.toString());
				    			
				    			
				    			String action = "moved";
				    			if (eActionSelected == eAction.eCopy) {
				    				action = "copied";
				    			}
				    			String logMsg = "MessageId " + msg.getMessageId() + " (replication id='" + replicationId.toString() + "') was " + action + 
				    					" from the '" + srcQName + "' queue to the '" + tarQName + "'.";
				    			CommandLog.instance().log(logMsg);
				    			if (eActionSelected == eAction.eMove) {
				    				axeIt = true;
				    			}
							}
							else {
				    			String logMsg = "MessageId " + msg.getMessageId() + " was deleted from the '" + srcQName + "' queue.";
				    			CommandLog.instance().log(logMsg);
			    				axeIt = true;
							}
							
							if (axeIt) {
								msg.ackMessage();
							}

			    			msgsProcessed++;
						    publish(i); // Send progress updates
						}
						else {
							// all done
						}
					} catch (BrokerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (int chunk : chunks) {
					double done = ((double) chunk / totalMsgCount);
					double dpercent = done * 100.0;
					int perc = (int) dpercent; 
				    progressBar.setValue(perc); // Update progress bar
					progressLabel.setText(getProgressLabelText());
                }
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    System.out.println("Task was canceled.");
                } else {
                    frame.dispose();
                }
            }
        };

        worker.execute(); // Start the task
		frame.setVisible(true);
	}

	private String getProgressLabelText() {
		String rc = "";
		String action = "";
		if (this.eActionSelected == eAction.eCopy) {
			action = "Copied";
		}
		else if (this.eActionSelected == eAction.eMove) {
			action = "Moved";
		}
		else if (this.eActionSelected == eAction.eDelete) {
			action = "Deleted";
		}
		rc = action + " " + this.msgsProcessed + " of " + this.totalMsgCount + " messages";
		return rc;
	}

//	public static void main(String[] args) {
//		CopyAllWindow me = new CopyAllWindow();
//		me.run();
//	}
}