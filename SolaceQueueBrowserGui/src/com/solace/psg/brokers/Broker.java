package com.solace.psg.brokers;


public class Broker {
	public String name;
	public String sempHost = "";
	public String sempAdminUser = "";
	public String sempAdminPw = "";
	public String msgVpnName = "";
	public String messagingClientUsername = "";
	public String messagingPw = "";
	public String messagingHost;
	
	public String fqdn() {
		String[]  parts = this.sempHost.split(":");
		String rc = parts[1];
		rc = rc.replaceAll("/", "");
		return rc;
	}
}
