package com.solace.psg.queueBrowser.gui.headers;


public class HeaderField {
	public enum eHeaderType {eFreeText, ePickList, eNumeric, eBoolean};

	public String name;
	public eHeaderType eType = eHeaderType.eFreeText; 
	public String textValue;
	
	public static HeaderField headerFactory(String name, eHeaderType type) {
		HeaderField rc = null;
		
		if (type.equals(eHeaderType.eFreeText)) {
			rc = new HeaderField();
		}
		else if (type.equals(eHeaderType.eBoolean)) {
			rc = new BooleanHeaderField();
		}
		else if (type.equals(eHeaderType.ePickList)) {
			rc = new PickListHeaderField();
		}
		else if (type.equals(eHeaderType.eNumeric)) {
			rc = new IntHeaderField();
		}
		
		rc.eType = type;
		rc.name = name;
		return rc;
	}
}
