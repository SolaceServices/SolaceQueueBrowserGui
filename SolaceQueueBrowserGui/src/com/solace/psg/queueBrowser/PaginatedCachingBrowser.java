package com.solace.psg.queueBrowser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.queueBrowser.gui.QueueBrowserMainWindow;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.TextMessage;

@SuppressWarnings("unused")
public class PaginatedCachingBrowser {
	private static final Logger logger = LoggerFactory.getLogger(PaginatedCachingBrowser.class.getName());
	private Broker broker;
	private String qToBrowse;
	private QueueBrowser solaceBrowserObject = null;
	private int paginationSize = 1;
	Map<String, BytesXMLMessage> allMessagesMap = new LinkedHashMap<String, BytesXMLMessage>();

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
		String payload = "";
		BytesXMLMessage message  = get(id);
		if (message instanceof TextMessage) {
			TextMessage txt = (TextMessage) message;
			payload = txt.getText();
		}
		else {
			byte[] b = message.getBytes();
			payload = new String(b);
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
		this.allMessagesMap.remove(id);
		
	}
	
	public void prefetchNextPage() throws BrokerException {
		int nCount = 0;
		String logReport = " fetching a page of messages, ids=";
		while (solaceBrowserObject.hasNext() && nCount < paginationSize) {
			nCount++;
			BytesXMLMessage message = solaceBrowserObject.next();
			
			@SuppressWarnings("deprecation")
			String id = message.getMessageId();
			allMessagesMap.put(id, message);
			logReport += id + ",";
		}
		logger.debug(logReport);
	}

}
