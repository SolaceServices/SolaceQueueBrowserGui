package com.solace.psg.queueBrowser.gui;

import java.util.ArrayList;

import com.solace.psg.queueBrowser.gui.headers.HeaderField;
import com.solace.psg.queueBrowser.gui.headers.IntHeaderField;
import com.solace.psg.queueBrowser.gui.headers.PickListHeaderField;

public class FilterSpecification {
	public ArrayList<HeaderField> headers = new ArrayList<HeaderField>();
	public FilterSpecification() {
		
		HeaderField header = HeaderField.headerFactory("Destination", HeaderField.eHeaderType.eFreeText);
		headers.add(header);

		PickListHeaderField pHeader = (PickListHeaderField) HeaderField.headerFactory("Delivery Mode", HeaderField.eHeaderType.ePickList);
		pHeader.selectedValue = "PERSISTENT";
		pHeader.values.add("DIRECT");
		pHeader.values.add("PERSISTENT");
		pHeader.values.add("NON_PERSISTENT");
		headers.add(pHeader);

		header = HeaderField.headerFactory("Reply-To Destination", HeaderField.eHeaderType.eFreeText);
		headers.add(header);

		IntHeaderField iHeader = (IntHeaderField) HeaderField.headerFactory("Time-To-Live (TTL)", HeaderField.eHeaderType.eNumeric);
		iHeader.value = 0;
		headers.add(iHeader);

		header = HeaderField.headerFactory("DMQ Eligible", HeaderField.eHeaderType.eBoolean);
		headers.add(header);

		header = HeaderField.headerFactory("Immediate Acknowledgement", HeaderField.eHeaderType.eBoolean);
		headers.add(header);

		header = HeaderField.headerFactory("Redelivery Flag", HeaderField.eHeaderType.eBoolean);
		headers.add(header);

		header = HeaderField.headerFactory("Deliver-To-One", HeaderField.eHeaderType.eBoolean);
		headers.add(header);

		pHeader = (PickListHeaderField) HeaderField.headerFactory("Class of Service (CoS)", HeaderField.eHeaderType.ePickList);
		for (int i=0; i<10;i++) {
			pHeader.values.add("     " + i);
		}
		headers.add(pHeader);
		
		header = HeaderField.headerFactory("Eliding Eligible", HeaderField.eHeaderType.eBoolean);
		headers.add(header);

		header = HeaderField.headerFactory("Message ID", HeaderField.eHeaderType.eFreeText);
		headers.add(header);

		header = HeaderField.headerFactory("Correlation ID", HeaderField.eHeaderType.eFreeText);
		headers.add(header);

		pHeader = (PickListHeaderField) HeaderField.headerFactory("Message Type", HeaderField.eHeaderType.ePickList);
		pHeader.values.add("Message (Generic, no body)");
		pHeader.values.add("Text");
		pHeader.values.add("Binary");
		pHeader.values.add("Stream");
		pHeader.values.add("Map");
		pHeader.values.add("Object");
		headers.add(pHeader);

		header = HeaderField.headerFactory("Encoding", HeaderField.eHeaderType.eFreeText);
		headers.add(header);

		/* no way to get this after message was sent, so cant qualify on it
		pHeader = (PickListHeaderField) HeaderField.headerFactory("Compression", HeaderField.eHeaderType.ePickList);
		for (int i=0; i<10;i++) {
			pHeader.values.add("     " + i);
		}
		headers.add(pHeader);
		*/

		/*
		"Destination","Delivery Mode", "Reply-To Destination", "Time-To-Live (TTL)",
		"DMQ Eligible", "Immediate Acknowledgement", "Redelivery Flag",
		"Deliver-To-One", "Class of Service (CoS)", "Eliding Eligible",
		"Message ID", "Correlation ID", "Message Type", "Encoding", "Compression"
		*/
	}
	
	public 	HeaderField find(String name) {
		HeaderField  rc = null;
		for (HeaderField f : headers) {
			if (f.name.equals(name)) {
				rc = f;
				break;
			}
		}
		return rc;
	}
	public enum FilterCondition {
	    NONE("-no filter-"),
	    EQUALS("equals"),
	    CONTAINS("contains"),
	    DOESNOTCONTAIN("does not contain");
	
	    private final String label;
	
	    FilterCondition(String label) {
	        this.label = label;
	    }
	
	    public String getLabel() {
	        return label;
	    }
	
	    @Override
	    public String toString() {
	        return label;
	    }
	}
	
	public FilterCondition headerCondition = FilterCondition.NONE;
	public FilterCondition userPropCondition = FilterCondition.NONE;
	public FilterCondition bodyCondition = FilterCondition.NONE;
	public String headerField = "";
	public String userPropField;
	public String headerValue;
	public String userPropValue;
	public String bodyValue;
	public String getDefaultHeaderFieldSelection() {return "Destination";}

	public boolean isEmpty() {
		return ((headerCondition == FilterCondition.NONE) && (userPropCondition == FilterCondition.NONE) && (bodyCondition == FilterCondition.NONE));
	}
}