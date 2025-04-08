package com.solace.psg.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TolerantHostnameVerifier implements HostnameVerifier {
	private static final Logger logger = LoggerFactory.getLogger(TolerantHostnameVerifier.class.getName());
	@Override
	public boolean verify(String arg0, SSLSession arg1) {
		logger.debug("HTTP hostname Verification... sure, looks good: " + arg0 + ", " + arg1.toString());
		return true;
	}
}
