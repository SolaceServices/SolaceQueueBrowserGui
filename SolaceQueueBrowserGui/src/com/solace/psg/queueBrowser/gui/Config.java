package com.solace.psg.queueBrowser.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.solace.psg.brokers.Broker;
import com.solace.psg.brokers.BrokerException;
import com.solace.psg.util.FileUtils;

public class Config {
	
	private String configFile;
	public Broker broker;
	public String downloadFolder = "./downloads";

	/* removed for V1 release
	public List<String> blackListedQueues = null;
	public List<String> whiteListedQueues = null;
	public HashMap<String, MovementDestination> movements = new HashMap<String, MovementDestination>(); 
	*/
	class MovementDestination {
		String source = "";
		List<String> destinations = new ArrayList<String>();
	}
	
	public Config(String file) {
		this.configFile = file;
	}
	public void load() throws BrokerException  {
	    String fileContent = null;
		try {
			fileContent = FileUtils.loadFile(this.configFile);
		} catch (IOException e) {
			throw new BrokerException(e);
		}
		JSONObject doc = new JSONObject(fileContent);
		if (doc.has("downloadFolder")) {
			downloadFolder = doc.getString("downloadFolder");
		}

		JSONObject eventBroker = doc.getJSONObject("eventBroker");
		
		broker = new Broker();
		broker.sempHost = eventBroker.getString("sempHost");
        broker.msgVpnName = eventBroker.getString("msgVpnName");
        broker.sempAdminUser = eventBroker.getString("sempAdminUser");
        broker.sempAdminPw = eventBroker.getString("sempAdminPw");
        broker.name = eventBroker.getString("name");
        
		broker.messagingHost = eventBroker.getString("messagingHost");
        broker.messagingClientUsername = eventBroker.getString("messagingClientUsername");
        broker.messagingPw = eventBroker.getString("messagingPw");
        
        /* This code removed for V1 release
         * 
        if (doc.has("blackListedQueues") && doc.has("whiteListedQueues")) {
        	throw new BrokerException("Invalid configuration. You cannot specify both a blacklist and a whitelist");
        }
        blackListedQueues = loadList(doc, "blackListedQueues");
        whiteListedQueues = loadList(doc, "whiteListedQueues");
        
        if (doc.has("movementDestinations")) {
        	JSONArray arr = doc.getJSONArray("movementDestinations");
        	for (int i = 0; i < arr.length(); i++) {
        		JSONObject oneDestinationJson = arr.getJSONObject(i);
        		
        		MovementDestination oneMover = new MovementDestination();
        		oneMover.source = oneDestinationJson.getString("source");
        		
        		JSONArray targetsArr = oneDestinationJson.getJSONArray("targets");
        		
            	for (int i2 = 0; i2 < targetsArr.length(); i2++) {
                    String target = targetsArr.getString(i2);
                    oneMover.destinations.add(target);
            	}
            	if (movements.containsKey(oneMover.source)) {
            		throw new BrokerException("Invalid configuration. Multiple entries exist for movementDistinations for queue '" + oneMover.source + "'.");
            	}
            	this.movements.put(oneMover.source, oneMover);
        	}        	
        }
        */
	}
	/*
	private List<String> loadList(JSONObject doc, String field) {
		List<String> rc = null; 
        if (doc.has(field)) {
        	rc = new ArrayList<String>();
        	JSONArray arr = doc.getJSONArray(field);
        	for (int i = 0; i < arr.length(); i++) {
                String value = arr.getString(i);
                rc.add(value);
            }
        }
        return rc;
	}
	*/
}
