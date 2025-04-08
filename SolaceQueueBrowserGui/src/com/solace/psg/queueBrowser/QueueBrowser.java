package com.solace.psg.queueBrowser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solacesystems.jcsmp.*;
/**
 * This class is a simple wrapper around a Solace browser object, build to use with the Broker 
 * class and optimized for flexible pagination useful in GUIs  
 *
 * @author Mike O'Brien
 */
public class QueueBrowser {
	private static final Logger logger = LoggerFactory.getLogger(QueueBrowser.class.getName());
	private Broker broker;
	private String qToBrowse;
	private Browser solaceBrowserObject = null;
	private JCSMPSession solaceJcsmpSession = null;
	private BytesXMLMessage preFetchedNextMessage = null;
	private int paginationSize = 1;

	public QueueBrowser(Broker broker, String qToBrowse) {
		this.broker = broker;
		this.qToBrowse = qToBrowse;
	}

	public QueueBrowser(Broker broker, String qToBrowse, int paginationSize) {
		this.broker = broker;
		this.qToBrowse = qToBrowse;
		this.paginationSize = paginationSize;
	}

	private void init() throws BrokerException {
		try {
			// Define connection properties
			JCSMPProperties properties = new JCSMPProperties();
			properties.setProperty(JCSMPProperties.HOST, this.broker.messagingHost);
			properties.setProperty(JCSMPProperties.VPN_NAME, this.broker.msgVpnName);
			properties.setProperty(JCSMPProperties.USERNAME, this.broker.messagingClientUsername);
			properties.setProperty(JCSMPProperties.PASSWORD, this.broker.messagingPw);

			// Create a session
			solaceJcsmpSession = JCSMPFactory.onlyInstance().createSession(properties);
			solaceJcsmpSession.connect();
			logger.debug("Connected to Solace via JCSMP to " + this.broker.messagingHost);
			
			// Define the queue object
			Queue queue = JCSMPFactory.onlyInstance().createQueue(this.qToBrowse);

			// Create a jcsmp browser object
			BrowserProperties browserProps = new BrowserProperties();
			browserProps.setEndpoint(queue);

			// Set the window size for browsing. this allows the underlying API to pre-fetch
			// the specified # of msgs
			// If used from a GUI, this should be set to the number of messages on a page.
			browserProps.setTransportWindowSize(paginationSize);
			solaceBrowserObject = solaceJcsmpSession.createBrowser(browserProps);
			logger.debug("Created a queue browser object sucessfully on queue '" + this.qToBrowse + "'.");

		} catch (JCSMPException e) {
			throw new BrokerException(e);
		}
	}

	private void lazyInit() throws BrokerException {
		if (solaceBrowserObject == null) {
			try {
				this.init();
			} catch (BrokerException e) {
				throw new BrokerException(e);
			}
		}
	}

	public boolean hasNext() throws BrokerException {
		lazyInit();
		try {
			preFetchedNextMessage = solaceBrowserObject.getNext(1000);
			
			if (preFetchedNextMessage != null) {
				@SuppressWarnings("deprecation")
				String id = preFetchedNextMessage.getMessageId();
				logger.debug("Fetched a msg, id =" + id);
			}
		} catch (JCSMPException e) {
			throw new BrokerException(e);
		}
		return (preFetchedNextMessage != null);
	}

	public BytesXMLMessage next() throws BrokerException {
		if (preFetchedNextMessage == null) {
			throw new BrokerException("No message to fetch");
		}
		return preFetchedNextMessage;
	}

	@SuppressWarnings("unused")
	private void close() {
		solaceBrowserObject.close();
		solaceJcsmpSession.closeSession();
	}

	/* if you want to run a text based standalone, uncomment from here to the end of main()
	 * 
	public void run() throws BrokerException {
		lazyInit();

		// Browse messages
		System.out.println("Browsing messages...");
		PromptingClient prompter = new PromptingClient();
		while (hasNext()) {
			BytesXMLMessage message = next();
			if (message == null) {
				break;
			}

			System.out.println("MessageID: " + message.getMessageId());

			if (message instanceof TextMessage) {
				TextMessage txt = (TextMessage) message;
				System.out.println("MessageID: " + txt.getText());
			}
			String action;
			try {
				action = prompter.prompt("Enter the action you want:");
			} catch (IOException e) {
				throw new BrokerException(e);
			}
			if (action.toLowerCase().equals("n")) {
				// just continue top ofloop
			} else if (action.toLowerCase().equals("d")) {
				message.ackMessage();
			}
			// else if (action.toLowerCase().equals("m")) {
			// ReplicationGroupMessageId id = message.getReplicationGroupMessageId();
			// String strID = id.toString();
			// copy("testbroker", "TestBrokerQ1", "DestQueue", strID);
			// message.ackMessage();
			// next = true;
			// }
			else if (action.toLowerCase().equals("q")) {
				break;
			}
			System.out.println("----------");
		}

		// Close the browser and session
		this.close();
	}


	public static void main(String[] args) throws BrokerException  {
		Broker b = new Broker();
		b.sempHost = "tcps://mr-connection-XXXXX.messaging.solace.cloud:55443";
		b.msgVpnName = "testbroker";
		b.sempAdminUser = "solace-cloud-client";
		b.sempAdminPw = "XXXXX";
		b.name = "Test broker";

		QueueBrowser me = new QueueBrowser(b, "TestBrokerQ1");
		me.run();
	}
	*/
}
