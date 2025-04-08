package com.solace.psg.brokers;

import com.solace.psg.brokers.semp.SempV1LegacyClient;

public class Broker {
	public SempV1LegacyClient semp = null;
	public String name;
	public String sempHost = "";
	public String sempAdminUser = "";
	public String sempAdminPw = "";
	public String msgVpnName = "";
	public String messagingClientUsername = "";
	public String messagingPw = "";
	public String messagingHost;
}
