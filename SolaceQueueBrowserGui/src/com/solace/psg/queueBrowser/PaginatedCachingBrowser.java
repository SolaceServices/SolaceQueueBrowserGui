package com.solace.psg.queueBrowser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.queueBrowser.gui.FilterSpecification;
import com.solace.psg.queueBrowser.gui.QueueBrowserMainWindow;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLContentMessage;
import com.solacesystems.jcsmp.impl.JCSMPGenericXMLMessage;

@SuppressWarnings("unused")
public class PaginatedCachingBrowser {
	private static final Logger logger = LoggerFactory.getLogger(PaginatedCachingBrowser.class.getName());
	private Broker broker;
	private String qToBrowse;
	private QueueBrowser solaceBrowserObject = null;
	private int paginationSize = 1;
	Map<String, BytesXMLMessage> allMessagesMap = new LinkedHashMap<String, BytesXMLMessage>();
	private FilterSpecification spec;

	public PaginatedCachingBrowser(Broker broker, String qToBrowse, int paginationSize) {
		super();
		this.broker = broker;
		this.qToBrowse = qToBrowse;
		this.paginationSize = paginationSize;
		
		this.solaceBrowserObject = new QueueBrowser(broker, qToBrowse, paginationSize);
	}
	
	public ArrayList<BytesXMLMessage> getPage(int nPage) {
		ArrayList<BytesXMLMessage> rc = new ArrayList<BytesXMLMessage>();
		
		int nStartingIndex = paginationSize * nPage;
		int nFinishIndex = paginationSize * (nPage + 1) - 1;
		
		int nCurrentIndex = 0;
		for (Entry<String, BytesXMLMessage> entry : allMessagesMap.entrySet()) {
            if ((nCurrentIndex >= nStartingIndex) && (nCurrentIndex <= nFinishIndex)) {
            	BytesXMLMessage obj = entry.getValue();
            	rc.add(obj);
            }
            if (nCurrentIndex == nFinishIndex) {
            	break;
            }
            else {
                nCurrentIndex++;
            }
		}
		return rc;
	}
	
	public String getPayload(String id) {
		BytesXMLMessage message  = get(id);
		return getPayload(message);
	}
	public String getPayload(BytesXMLMessage message) {
		String payload = "";
		if (message instanceof TextMessage) {
			TextMessage txt = (TextMessage) message;
			payload = txt.getText();
		}
		else if (message instanceof BytesMessage) {
			BytesMessage by = (BytesMessage) message;
			byte[] b = by.getData(); 
			if (b != null) {
				payload = new String(b);
			}
			else {
				byte[] bytes = by.getBytes();
				payload = new String(bytes); 
			}
		}
		else if (message instanceof StreamMessage) {
			try {
				StreamMessage streamM = (StreamMessage) message; 
			    SDTStream stream = streamM.getStream();
	
			    StringBuilder builder = new StringBuilder();
			    while (stream.hasRemaining()) {
			        Object element;
						element = stream.read();
			        builder.append(element.toString());
			        builder.append(" "); // optional separator
			    }
			    payload = builder.toString().trim();
			} catch (SDTException e) {
				e.printStackTrace();
			}
		}
		else if (message instanceof MapMessage) {
			MapMessage mapM = (MapMessage) message;
		    SDTMap map = mapM.getMap();

		    StringBuilder builder = new StringBuilder();
		    Set<String> set = map.keySet();
		    for (String key : set) {
		        Object value = null;
				try {
					value = map.get(key);
				} catch (SDTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        builder.append(key)
		               .append(": ")
		               .append(value.toString()) 
		               .append("\n");
		    }
			payload = builder.toString().trim();
		}
	    else if (message instanceof XMLContentMessage) {
	    	XMLContentMessage xml = (XMLContentMessage) message;
	    	if (xml.hasAttachment()) {
		    	ByteBuffer binaryPayload = xml.getAttachmentByteBuffer();
		    	byte[] bytes = new byte[binaryPayload.remaining()];
		    	binaryPayload.get(bytes);
		    	payload = new String(bytes, StandardCharsets.UTF_8);
	    	}
	    	else {
	    		payload = xml.getXMLContent();
	    	}
	    }

		return payload;
	}
	
	public BytesXMLMessage get(String id) {
		return this.allMessagesMap.get(id);
	}
	public void delete(String id) {
		BytesXMLMessage m = get(id);
		if (m != null) {
			m.ackMessage();
		}
		remove(id);
	}
	public void remove(String id) {
		this.allMessagesMap.remove(id);
	}

	public boolean hasMoreAfterId(String lastIdAdded) {
		return this.solaceBrowserObject.hasMoreAfterId(lastIdAdded);
	}

	public void prefetchNextPage() throws BrokerException {
		int nCount = 0;
		String logReport = " fetching a page of messages, ids=";
		while (solaceBrowserObject.hasNext() && nCount < paginationSize) {
			BytesXMLMessage message = solaceBrowserObject.next();
			
			@SuppressWarnings("deprecation")
			String id = message.getMessageId();
			
			if (thisMessageMatchesTheFilter(message)) {
				nCount++;
				allMessagesMap.put(id, message);
			}
			logReport += id + ",";
		}
		logger.debug(logReport);
	}
	
	private boolean stringQualificationMatch(String value1, String value2, FilterSpecification.FilterCondition condition) {
		boolean bRc = false;
		if (condition.equals(FilterSpecification.FilterCondition.EQUALS)) {
			bRc = value1.equals(value2);
		}
		else if (condition.equals(FilterSpecification.FilterCondition.CONTAINS)) {
			bRc = value1.contains(value2);
		}
		else if (condition.equals(FilterSpecification.FilterCondition.DOESNOTCONTAIN)) {
			bRc = ! value1.contains(value2);
		}
		return bRc;
	}
	
	private boolean thisMessageMatchesTheHeaderFilter(BytesXMLMessage message) {
		boolean bRc = false;

		logger.debug("filter=" + this.spec.headerField + " with action=" + this.spec.headerCondition + "=" + this.spec.headerValue);
		
		if (this.spec.headerField.equals("Destination")) {
			String msgDest = message.getDestination().getName();
			bRc = stringQualificationMatch(msgDest, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("destinatin=" + msgDest + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Delivery Mode")) {
			String msgMode = message.getDeliveryMode().name();
			bRc = stringQualificationMatch(msgMode, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("Delivery Mode=" + msgMode + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Reply-To Destination")) {
			Destination dest = message.getReplyTo();
			if (dest != null) {
				String msgDest = dest.getName();
				bRc = stringQualificationMatch(msgDest, this.spec.headerValue, this.spec.headerCondition);
				logger.debug("Reply-To Destination=" + msgDest + "... passes=" + bRc);
			}
			else {
				bRc = false;
				logger.debug("Reply-To Destination=NULL ... passes=" + bRc);
			}
		}
		else if (this.spec.headerField.equals("Time-To-Live (TTL)")) {
			long ttl = message.getTimeToLive();
			long ttlFilter = Long.parseLong(this.spec.headerValue);
			bRc = (ttl == ttlFilter);
			logger.debug("TTL=" + ttl + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("DMQ Eligible")) {
			boolean boolMsg = message.isDMQEligible();
			boolean boolFilter = Boolean.parseBoolean(this.spec.headerValue);
			bRc = (boolMsg == boolFilter);
			logger.debug("DMQ Eligible =" + boolMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Immediate Acknowledgement")) {
			boolean boolMsg = message.isAckImmediately();
			boolean boolFilter = Boolean.parseBoolean(this.spec.headerValue);
			bRc = (boolMsg == boolFilter);
			logger.debug("Immediate Acknowledgement=" + boolMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Redelivery Flag")) {
			boolean boolMsg = message.getRedelivered();
			boolean boolFilter = Boolean.parseBoolean(this.spec.headerValue);
			bRc = (boolMsg == boolFilter);
			logger.debug("Redelivery Flag=" + boolMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Deliver-To-One")) {
			boolean boolMsg = message.getDeliverToOne();
			boolean boolFilter = Boolean.parseBoolean(this.spec.headerValue);
			bRc = (boolMsg == boolFilter);
			logger.debug("Deliver-To-One=" + boolMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Class of Service (CoS)")) {
			int intMsg = message.getCos().ordinal();
			int intFilter = Integer.parseInt(this.spec.headerValue);
			bRc = (intMsg == intFilter);
			logger.debug("Class of Service (CoS)=" + intMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Eliding Eligible")) {
			boolean boolMsg = message.isElidingEligible();
			boolean boolFilter = Boolean.parseBoolean(this.spec.headerValue);
			bRc = (boolMsg == boolFilter);
			logger.debug("Eliding Eligible=" + boolMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Message ID")) {
			String strMsg = message.getMessageId();
			bRc = stringQualificationMatch(strMsg, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("Message ID=" + strMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Correlation ID")) {
			String strMsg = message.getCorrelationId();
			bRc = stringQualificationMatch(strMsg, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("Correlation ID=" + strMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Message Type")) {
			String strMsg = message.getMessageType().toString();
			bRc = stringQualificationMatch(strMsg, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("Message Type=" + strMsg + "... passes=" + bRc);
		}
		else if (this.spec.headerField.equals("Encoding")) {
			String strMsg = message.getHTTPContentEncoding();
			bRc = stringQualificationMatch(strMsg, this.spec.headerValue, this.spec.headerCondition);
			logger.debug("Encoding=" + strMsg + "... passes=" + bRc);
		}

//			"Destination","Delivery Mode", "Reply-To Destination", "Time-To-Live (TTL)",
//			"DMQ Eligible", "Immediate Acknowledgement", "Redelivery Flag",
//			"Deliver-To-One", "Class of Service (CoS)", "Eliding Eligible",
//			"Message ID", "Correlation ID", "Message Type", "Encoding", "Compression"s
		return bRc;	
	}
	private boolean thisMessageMatchesTheUserPropFilter(BytesXMLMessage message) {
		boolean bRc = false;
		String propNamedInFilter = this.spec.userPropField;
		String propToSearchFor = propNamedInFilter.toLowerCase(); 
		String valueInFilter = this.spec.userPropValue;
		String propValueFound = "";
		
		boolean foundIt = false;
		// first, do we even have this prop? 
		SDTMap map = message.getProperties();
		Set<String> keys = map.keySet();
		for (String key : keys) {
	        if (key.toLowerCase().equals(propToSearchFor)) {
	        	foundIt = true;
	        	try {
					propValueFound = map.get(key).toString();
				} catch (SDTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	break;
	        }
		}
		if (foundIt) {
			propValueFound = propValueFound.toLowerCase();
			valueInFilter = valueInFilter.toLowerCase();
			bRc = stringQualificationMatch(propValueFound, valueInFilter, this.spec.userPropCondition);
		}
		else {
			// it wasnt found.. but that could still satisfy a negative filter
			if (this.spec.userPropCondition.equals(FilterSpecification.FilterCondition.DOESNOTCONTAIN)) {
				bRc = true;
			}
		}
		return bRc;
	}

	private boolean thisMessageMatchesTheFilter(BytesXMLMessage message) {
		boolean bRc = false;

		boolean bPassesHeader = true;
		boolean bPassesUserProps = true;
		boolean bPassesBody = true;

		// first filter by headers. since fetching payload is heavier, save that until last 
		// because we might eliminate some rows first 
		if (this.spec.headerCondition.equals(FilterSpecification.FilterCondition.NONE) == false) {
			// we have a header spec
			bPassesHeader = thisMessageMatchesTheHeaderFilter(message);
		}

		if (this.spec.userPropCondition.equals(FilterSpecification.FilterCondition.NONE) == false) {
			// we have a header spec
			bPassesHeader = thisMessageMatchesTheUserPropFilter(message);
		}

		if ((this.spec.bodyValue != null) && (this.spec.bodyValue.isEmpty() == false)) {
			String payload = this.getPayload(message);
			bPassesBody = stringQualificationMatch(payload, this.spec.bodyValue, this.spec.bodyCondition);
		}
		bRc = (bPassesHeader && bPassesUserProps && bPassesBody);
		return bRc;
	}

	public void setFilter(FilterSpecification spec) {
		this.spec = spec;
	}


}
