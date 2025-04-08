package com.solace.psg.brokers.semp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.http.HttpClient;

public class SempV1LegacyClient {
	class ResponseAndCookie {
		String response;
		String moreCookie;
	}

	private static final Logger logger = LoggerFactory.getLogger(SempV1LegacyClient.class.getName());
	public String fullv1Url = "";
	public String user = "";
	public String pw = "";
	private AtomicInteger queryCount = new AtomicInteger(1);
	
	public SempV1LegacyClient(String sempV2Url, String user, String pw) {
		this.fullv1Url = sempV2Url.replace("/v2/config", "");
		this.fullv1Url = fullv1Url.replace("/v2/monitor", "");
		this.user = user;
		this.pw = pw;
	}
	
	private boolean isASempV1Sucess(String body) {
		String ok = "<execute-result code=\"ok\"/>";
		return (body.contains(ok));
	}
	
	private ResponseAndCookie removeAndGetCookie(String response) {
		ResponseAndCookie rc = new ResponseAndCookie();
		rc.moreCookie = SempV1LegacyClient.getElement(response, "more-cookie");
		response = response.replace(rc.moreCookie, "");  
		response = response.replace("<more-cookie>", "");  
		response = response.replace("</more-cookie>", "");  
		rc.response = response;
		return rc;
	}
	
	public List<String> getSempV1(String xmlBody, String pathToRepeatingItem) throws SempException {
		int thisQueryPk = queryCount.getAndIncrement();
		List<String> objectsToReturn = new ArrayList<String>();
		
		String[] levels = pathToRepeatingItem.split("/");
		String leaf = levels[levels.length - 1];
		
		// see if this leaf is found multiple times
		int nCount = 0;
		for (String level: levels) {
			if (level.equals(leaf)) {
				nCount++;
			}
		}
		
		logger.info("Query " + thisQueryPk + " calling Semp v1 on " + fullv1Url + " body=" + xmlBody);
		String response;
		try {
			response = HttpClient.postWithBody(fullv1Url, user, pw, xmlBody, 3000, HttpClient.XML);
			logger.info("Query " + thisQueryPk + " returns body=" + response);
			
			if (response.contains("<more-cookie>")) {
				logger.debug("Entering pagination handling for Query " + thisQueryPk);
				ResponseAndCookie result = removeAndGetCookie(response);
				response = result.response;
				logger.debug("Query " + thisQueryPk + " Initial cookie=" + result.moreCookie);
				List<String> objects = SempV1LegacyClient.getRepeatingItem(result.response, leaf, nCount);
				objectsToReturn.addAll(objects);

				boolean gotAllPages = false;
				int nPage = 1; // we already did that
				while (gotAllPages == false) {
					nPage++;
					String subResponse = HttpClient.postWithBody(fullv1Url, user, pw,  result.moreCookie, 3000, HttpClient.XML);
					logger.info("Query " + thisQueryPk + "." + nPage + " pagination sub-query returns body=" + subResponse);
					if (subResponse.contains("<more-cookie>") == false) {
						gotAllPages = true;
					}
					else {
						result = removeAndGetCookie(subResponse);
						logger.debug("Cookie from page " + nPage + " cookie=" + result.moreCookie);
						subResponse = result.response;  
					}
					objects = SempV1LegacyClient.getRepeatingItem(response, leaf, nCount);
					objectsToReturn.addAll(objects);
				}
				logger.debug("Query " + thisQueryPk + " pagination handling comepleted. There is no cookie in the last query.");
			}
			else {
				List<String> objects = SempV1LegacyClient.getRepeatingItem(response, leaf, nCount);
				objectsToReturn.addAll(objects);
			}
		} 
		catch (IOException e) {
			throw new SempException(e);
		}
		if (isASempV1Sucess(response) == false) {
			throw new SempException(response);
		}
		logger.debug("Query " + thisQueryPk + " returns " + objectsToReturn.size() + " semp objects.");
		return objectsToReturn;
	}
	
	
	public static List<String> getRepeatingItem(String response, String xmlElement) {
		return getRepeatingItem(response, xmlElement, 1);
	}
	public static List<String> getRepeatingItem(String response, String xmlElement, int levelsDeep) {
		if (levelsDeep > 2) {
			throw new RuntimeException("Mini-xml parser only supports 2 levels");
		}
		List<String> rc = new ArrayList<String>();
		int currentIndex = 0;
		boolean finished = false;
		
		String find = "<" + xmlElement + ">";
		int outerIndex = response.indexOf(find, currentIndex);
		if ((outerIndex > -1) && (levelsDeep == 2)) {
			currentIndex = outerIndex + 1;
		}
		
		while (!finished) {
			int startIndex = response.indexOf(find, currentIndex);
			if (startIndex > -1) {
				// move up past the string we looked for
				int containedStart = startIndex + find.length();
				
				// look for the next closing 
				String findClosing = "</" + xmlElement + ">";
				int containedEnd = response.indexOf(findClosing, currentIndex);
				String oneItem = response.substring(containedStart, containedEnd);
				
				// move up past the end of this block
				currentIndex = containedEnd + find.length();
				rc.add(oneItem);
			} else {
				finished = true;
			}
		}
		return rc;
	}
	
	public static boolean getBooleanElement(String response, String xmlElement) {
		String element = getElement(response, xmlElement);
		element = element.toLowerCase();
		return (element.equals("true") || element.equals("yes"));
	}
	public static boolean getYesNoBooleanElement(String response, String xmlElement) {
		String element = getElement(response, xmlElement);
		element = element.toLowerCase();
		return element.equals("Yes");
	}
	public static int getIntElement(String response, String xmlElement) {
		String element = getElement(response, xmlElement);
		element = element.toLowerCase();
		return Integer.parseInt(element);
	}

	public static String getElement(String response, String xmlElement) {
		String rc = "";
		String find = "<" + xmlElement + ">";
		int startIndex = response.indexOf(find);
		if (startIndex > -1) {
			int containedStart = startIndex + find.length();
			
			find = "</" + xmlElement + ">";
			int containedEnd = response.indexOf(find);
			rc = response.substring(containedStart, containedEnd);
		}
		return rc;
	}
}