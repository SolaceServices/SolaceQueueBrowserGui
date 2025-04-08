package com.solace.psg.brokers.semp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.solace.psg.http.HttpClient;

public class SempClient {
	public enum ePaginationBehavior {eNone, ePaged};
	private static final Logger logger = LoggerFactory.getLogger(SempClient.class.getName());

	public String host, user, pw;
	public String fullUrl;
	private static Gson gson = new Gson();
	private boolean verifyHostnameOnSllHandshake = true; 

	public SempClient(String url, String user, String pw) {
		this.fullUrl = url;
		this.user = user;
		this.pw = pw;
	}
	public void dontVerifyHostnameOnSllHandshake() {
		verifyHostnameOnSllHandshake = false;
	}
	public static String toJson(Map<String, Object> map) {
		return gson.toJson(map);
	}
	public static boolean isA200Response(String responseText) {
		boolean bIs200 = false;
		JSONObject obj = new JSONObject(responseText);
	    
		logger.debug("converted response to json Obj");
	    if (obj.has("meta")) {
			logger.debug("got meta obj");
	        JSONObject metaObj = obj.getJSONObject("meta");
	    	// a problem creating the service
	        int nReturnCode = metaObj.getInt("responseCode");
			logger.debug("code=" + nReturnCode);
	    	if (nReturnCode == 200)  {
				logger.debug("Its a 200");
	    		bIs200 = true;
	    	}
	    }
        return bIs200;
	}

//	public static String hostPortionOfUrl(String url) {
//		int nIndexStart = url.indexOf("//") + 2; //2 = length("//");
//		
//		int nIndexEnd = url.indexOf(":");
//		nIndexEnd = url.indexOf(":", nIndexEnd + 1); // the port will be delineated from the host by the second colon
//		
//		return url.substring(nIndexStart, nIndexEnd);
//	}
//	private static String protocol(boolean bSecure) {
//		String rc = "http";
//		if (bSecure) {
//			rc = "https";
//		}
//		return rc;
//	}
	
//	private String getSempV2Url(String host, boolean bSecure, String resource) {
//		String version = "v2";
//		if (resource.startsWith("/") == false) {
//			version += "/";
//		}
//		String url = protocol(bSecure) + "://" + host + ":"  + port + "/SEMP/" + version + resource;
//		url = url.replace("config/config/", "config/");
//		return url;
//	}
	
//	public void test(String vpn, String queue) throws SempException, IOException {
//		//String thisUrl = this.fullUrl.replace("config", "monitor");
//		String thisUrl = "/msgVpns/" + vpn + "/queues/" + queue + "/msgs?select=msgId,replicationGroupMsgId";
//		String response = this.getSempV2Monitoring(thisUrl, ePaginationBehavior.ePaged);
//	}
	
	/*
	private void copy(String vpn, String fromQ, String toQ, String id) throws SempException {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("replicationGroupMsgId", id);
		
//		String jsonBody = toJson(map);
//		String thisUrl = this.fullUrl.replace("config", "monitor", 0) + resource;
//		return this.putVpnObjectFullUrl(thisUrl, jsonBody);
//
		
		String url = "/msgVpns/" + vpn + "/queues/" + toQ + "/copyMsgFromQueue";
		putVpnObject(url, map);
	}
	*/
	
	public void delete(String resource) throws SempException  {
		String urlToUse = this.fullUrl + resource; 
		logger.info("Deleteing SEMP entity at " + urlToUse);
		try {
			HttpClient.delete(urlToUse, user, pw);
		} catch (IOException e) {
			throw new SempException(e);
		}
	}

	public String getSempV2Monitoring(String resource, ePaginationBehavior paginate) throws SempException, IOException {
		return this.getSempV2("monitor", resource, paginate);
	}

	public String getSempV2(String resource, ePaginationBehavior paginate) throws SempException, IOException {
		return this.getSempV2("config", resource, paginate);
	}
	private String getSempV2(String apiEndpoint, String resource, ePaginationBehavior paginate) throws SempException, IOException {
		String results = "";
		boolean finished = false;
		while (!finished) {
			String urlToUse = "";
			if (paginate == ePaginationBehavior.ePaged) {
				String paginated = "";
				String delim = "?";
				if (resource.contains("?")) {
					delim = "&";
				}
				paginated = resource + delim + "count=2"; //10000";
				urlToUse = this.fullUrl + paginated; //getSempV2Url(host, bSecure, paginated);
			}
			else {
				urlToUse = this.fullUrl + resource; // getSempV2Url(host, bSecure, resource);
				finished = true;
			}
			if (apiEndpoint.equals("monitor") ) {
				urlToUse = urlToUse.replace("config", "monitor");
			}
			
			logger.debug("trying the broker on Url " + urlToUse);
			logger.debug("Using admin creds" + this.user + "/" + this.pw);
			
			try {
				if (verifyHostnameOnSllHandshake == true) {
					//HttpClient.verify(false);
					logger.warn("The http implementation is currently hard coded to bypass SSL hostname validation. The verifyHostnameOnSllHandshake setting is not being respected.");
				}
				results = HttpClient.fetch(urlToUse, user, pw);
				logger.debug("http GET returns:" + results);
				finished = true;
			} catch (Exception e) {
				Throwable inner = e.getCause();
				String strErr = "";
				if (inner != null) {
					strErr = inner.getMessage();
				} else {
					strErr = e.getMessage();
				}
				if (strErr.contains("Collection does not support paging")) {
					paginate = ePaginationBehavior.eNone;
				}
				else {
					logger.error("failure", e);
					finished = true;
					if (inner != null) {
						throwSempError(e, strErr);
					}
					else {
						throw new SempException(e);
					}
				}
			} 
		}
		return results;
	}

	private void throwSempError(Throwable t, String responseText) throws SempException {
		//logger.debug("error response:" + responseText);
		JSONObject obj = new JSONObject(responseText);
		logger.debug("converted error response to json Obj");
	    if (obj.has("meta")) {
			logger.debug("got meta obj");
			JSONObject metaObj = obj.getJSONObject("meta");
		    if (metaObj.has("error")) {
		    	JSONObject errorObj = metaObj.getJSONObject("error");	
				logger.debug("got error obj");
		        int code = errorObj.getInt("code"); 
		        String desc = errorObj.getString("description");
		        String status = errorObj.getString("status");
		        throw new SempException("SEMP Error (" + code + "): " + status + ": " + desc);
		    } 
	    }
	    else {
	    	throw new SempException(t.getMessage());
	    }
	}
	

	private ArrayList<String> getDataObjectsElement(String responseText, String fieldName) {
		ArrayList<String> values = new ArrayList<String>();
		JSONObject obj = new JSONObject(responseText);
		logger.debug("converted response to json Obj");
	    if (obj.has("data")) {
			logger.debug("got data obj");
	        JSONArray dataObj = obj.getJSONArray("data");
	        for (int index = 0; index < dataObj.length(); index++) {
	        	JSONObject oneVpnObj = dataObj.getJSONObject(index);
	        	String oneStr = oneVpnObj.getString(fieldName);
	        	values.add(oneStr);
	        }
	    }
		return values;
	}
	@SuppressWarnings("unused")
	private int getDataObjectIntElement(String responseText, String fieldName) {
		int rc = -1;
		JSONObject obj = new JSONObject(responseText);
		logger.debug("converted response to json Obj");
	    if (obj.has("data")) {
			logger.debug("got data obj");
	        JSONObject dataObj = obj.getJSONObject("data");
	        rc = dataObj.getInt(fieldName);
	    }
		return rc;
	}
	private boolean getDataObjectBooleanElement(String responseText, String fieldName) {
		boolean rc = false;
		JSONObject obj = new JSONObject(responseText);
		logger.debug("converted response to json Obj");
	    if (obj.has("data")) {
			logger.debug("got data obj");
	        JSONObject dataObj = obj.getJSONObject("data");
	        rc = dataObj.getBoolean(fieldName);
	    }
		return rc;
	}

	public List<String> getList(String resource, String column) throws SempException {
		ArrayList<String> values = new ArrayList<String>();
		try {
			String responseText = this.getSempV2(resource, ePaginationBehavior.ePaged);
			values = this.getDataObjectsElement(responseText, column);
		} catch (SempException | IOException e) {
			throw new SempException(e);
		} 
		return values;
	}

	public List<String> getObjectNames(String vpn, String objectName, String identityColumn) throws SempException {
		ArrayList<String> values = new ArrayList<String>();
		String resource = "monitor/msgVpns/" + vpn + "/" + objectName + "?select=" + identityColumn;
		try {
			String responseText = this.getSempV2(resource, ePaginationBehavior.ePaged);
			values = this.getDataObjectsElement(responseText, identityColumn);
		} catch (SempException | IOException e) {
			throw new SempException(e);
		} 
		return values;
	}
	public String getObjectDetails( String vpn, String objectName, String identityColumn, String identityValue) throws SempException {
		@SuppressWarnings("deprecation")
		String primaryKey = URLEncoder.encode(identityValue);
		
		String resource = "monitor/msgVpns/" + vpn + "/" + objectName + "/" + primaryKey;
		String responseText = "";
		try {
			responseText = this.getSempV2(resource, ePaginationBehavior.eNone);
			
			//get rid of useless metadata
			JSONObject doc = new JSONObject(responseText);
			JSONObject data = doc.getJSONObject("data");
			responseText = data.toString();

		} catch (IOException e) {
			throw new SempException(e);
		}
		return responseText;
	}
	public List<String> getAllQueueNames(String vpn) throws SempException {
		ArrayList<String> values = new ArrayList<String>();
		String resource = "/msgVpns/{msgVpnName}/queues?select=queueName";
		resource = resource.replace("{msgVpnName}", vpn);
		
		try {
			String responseText = this.getSempV2(resource, ePaginationBehavior.ePaged);
			values = this.getDataObjectsElement(responseText, "queueName");
		} catch (SempException | IOException e) {
			throw new SempException(e);
		} 
		return values;
	}
	
	public String getQueueDetails( String vpn, String queueName) throws SempException {
		String resource = "config/msgVpns/{msgVpnName}/queues/{queueName}";
		resource = resource.replace("{msgVpnName}", vpn);
		resource = resource.replace("{queueName}", queueName);

		String responseText = "";
		try {
			responseText = this.getSempV2(resource, ePaginationBehavior.eNone);
			//enabled = this.getDataObjectBooleanElement(responseText, field);
		} catch (IOException e) {
			throw new SempException(e);
		}
		return responseText;
	}

	public void parseException(Exception e) throws SempException {
		String errorBody = e.getMessage();
		SempException se = null;
		if (errorBody.contains("\"meta\":")) {
			
			//System.out.println(errorBody);
			errorBody = errorBody.replace("java.lang.Exception: ", "");
			
			JSONObject doc = new JSONObject(errorBody);
			JSONObject meta = doc.getJSONObject("meta");
			int code = meta.getInt("responseCode");
			JSONObject error = meta.getJSONObject("error");
			int subCode = error.getInt("code");
			String desc = error.getString("description");
			String status = error.getString("status");
			
			String msg = "Error " + code + " (subcode=" + subCode + ", status=" + status + ") " + desc;
			se = new SempException(msg);
			se.code = code;
			se.subCode = subCode;
			se.description = desc;
			se.status = status;
		} else {
			se = new SempException(errorBody);
		}
		
		throw se; 
	}

	public String postVpnObject(String resource, String body) throws SempException {
		String responseText = "";
		try {
			String url = this.fullUrl + resource;
			logger.info("posting Semp request on " + url);
			logger.debug("body=" + body);
			responseText = HttpClient.postWithBody(url, this.user, this.pw, body, HttpClient.JSON);
		} catch (IOException e) {
			parseException(e);
		}
		return responseText;
	}
	
	private String postVpnObjectFullUrl(String fullUrl, String body) throws SempException {
		String responseText = "";
		try {
			logger.info("posting Semp request on " + fullUrl);
			logger.debug("body=" + body);
			responseText = HttpClient.postWithBody(fullUrl, this.user, this.pw, body, HttpClient.JSON);
		} catch (IOException e) {
			parseException(e);
		}
		return responseText;
	}
	public  String putVpnObject(String resource, Map<String, Object> bodyMap) throws SempException {
		String jsonBody = toJson(bodyMap);
		String thisUrl = this.fullUrl + resource;
		return this.putVpnObjectFullUrl(thisUrl, jsonBody);
	}
	
	public  String patchVpnObject(String resource, Map<String, Object> bodyMap) throws SempException {
		String jsonBody = toJson(bodyMap);
		String thisUrl = this.fullUrl + resource;
		return this.patchVpnObjectFullUrl(thisUrl, jsonBody);
	}
	private String patchVpnObjectFullUrl(String fullUrl, String body) throws SempException {
		String responseText = "";
		try {
			logger.info("patching Semp request on " + fullUrl);
			logger.debug("body=" + body);
			responseText = HttpClient.patchWithBody(fullUrl, this.user, this.pw, body, HttpClient.JSON);
		} catch (IOException e) {
			parseException(e);
		}
		return responseText;
	}
	private String putVpnObjectFullUrl(String fullUrl, String body) throws SempException {
		String responseText = "";
		try {
			logger.info("patching Semp request on " + fullUrl);
			logger.debug("body=" + body);
			responseText = HttpClient.putWithBody(fullUrl, this.user, this.pw, body, HttpClient.JSON);
		} catch (IOException e) {
			parseException(e);
		}
		return responseText;
	}
	
	public SempSaveResult postOrPatchVpnObject(String vpnUrl, String idAttributeValue, Map<String, Object> bodyMap) throws SempException {
		String json = toJson(bodyMap);
		return this.postOrPatchVpnObject(vpnUrl, idAttributeValue, json);
	}

	public eSempOperation postVpnObjectIgnoreIfExists(String vpnUrl, Map<String, Object> bodyMap) throws SempException {
		eSempOperation oper = eSempOperation.eAlreadyThereNoActionPossible;
		String json = toJson(bodyMap);
		String thisUrl = this.fullUrl + vpnUrl;
		try {
			postVpnObjectFullUrl(thisUrl, json);
			oper = eSempOperation.ePost;
		} catch (SempException e) {
			if (e.status.equals("ALREADY_EXISTS")) {
				logger.info("Semp request failed; ALREADY_EXISTS" + thisUrl);
				logger.info("No patch will be attempted");
			}
			else {
				throw new SempException(e);
			}
		}
		return oper;	
	}
//	public eSempOperation putVpnObject(String vpnUrl, Map<String, Object> bodyMap) throws SempException {
//		eSempOperation oper = eSempOperation.ePut;
//		String json = toJson(bodyMap);
//		String thisUrl = this.fullUrl + vpnUrl;
//		try {
//			putVpnObjectFullUrl(thisUrl, json);
//			oper = eSempOperation.ePost;
//		} catch (SempException e) {
//			if (e.status.equals("ALREADY_EXISTS")) {
//				logger.info("Semp request failed; ALREADY_EXISTS" + thisUrl);
//				logger.info("No patch will be attempted");
//			}
//			else {
//				throw new SempException(e);
//			}
//		}
//		return oper;	
//	}
	public enum eSempOperation {eNone, eAlreadyThereNoActionPossible, ePost, ePatch, ePut,eDelete};
	public class SempSaveResult {
		public eSempOperation operation = eSempOperation.eNone;
		public String ResponsePayload;
		public String actionPerformedVerb() {
			return SempClient.actionPerformedVerb(this.operation);
		}
	}
	public static String actionPerformedVerb(eSempOperation oper) {
		String rc = "Not touched";
		if (oper == eSempOperation.ePost) { rc = "created";}
		else if (oper == eSempOperation.ePatch) { rc = "updated";}
		else if (oper == eSempOperation.eAlreadyThereNoActionPossible) { rc = "left unchanged";}
		else if (oper == eSempOperation.ePatch) { rc = "updated (patch)";}
		else if (oper == eSempOperation.ePut) { rc = "updated (put)";}
		else if (oper == eSempOperation.eDelete) { rc = "deleted";}
		return rc;
		
	}
	public SempSaveResult postOrPatchVpnObject(String vpnUrl, String idAttributeValue, String json) throws SempException {
		SempSaveResult rc = new SempSaveResult();
		
		String thisUrl = this.fullUrl + vpnUrl;
		boolean patch = false;
		String responseText = "";
		try {
			rc.operation = eSempOperation.ePost;
			responseText = postVpnObjectFullUrl(thisUrl, json);
		} catch (SempException e) {
			if (e.status.equals("ALREADY_EXISTS")) {
				logger.info("Semp request failed; ALREADY_EXISTS - posting to " + thisUrl);
				patch = true;
			}
			else {
				throw new SempException(e);
			}
		}
		
		if (patch) {
			thisUrl += "/" + idAttributeValue;
			logger.info("Semp request falling back to patch " + thisUrl);
			rc.operation = eSempOperation.ePatch;
			responseText = patchVpnObjectFullUrl(thisUrl, json);
		}
		rc.ResponsePayload = responseText; 
		return rc;
	}

    public List<String> getAllVpnBridgeNameOnBroker(SempClient api, String vpn) throws SempException {
    	List<String> listRc = null;
        String resource = "/msgVpns/" + vpn + "/bridges?select=bridgeName";    
		try {
			String responseText = this.getSempV2(resource, ePaginationBehavior.ePaged);
			listRc = this.getDataObjectsElement(responseText, "bridgeName");
			
			if (listRc.contains("#ACTIVE")) {
				listRc.remove("#ACTIVE");
			}
		} catch (IOException e) {
			throw new SempException(e);
		}
        return listRc;
    }
	
	public boolean getQueueBoolean( String vpn, String queueName, String field) throws SempException {
		String resource = "monitor/msgVpns/{msgVpnName}/queues/{queueName}";
		resource = resource.replace("{msgVpnName}", vpn);
		resource = resource.replace("{queueName}", queueName);

		boolean enabled = false;
		try {
			String responseText = this.getSempV2(resource, ePaginationBehavior.eNone);
			enabled = this.getDataObjectBooleanElement(responseText, field);
		} catch (IOException e) {
			throw new SempException(e);
		}
		return enabled;
	}
	private int getMetaCount(String responseText) {
		int rc = -1;
		JSONObject obj = new JSONObject(responseText);
		logger.debug("converted response to json Obj");
	    if (obj.has("meta")) {
			logger.debug("got the meta");
	        JSONObject dataObj = obj.getJSONObject("meta");
	        rc = dataObj.getInt("count");
	    }
		return rc;
	}
	
	public List<String> getIdentityValues(String resource, String columnName, boolean expectingSingleValue) throws SempException {
		ArrayList<String> values = null;
		try {
			ePaginationBehavior paging = ePaginationBehavior.ePaged;
			if (expectingSingleValue) {
				paging = ePaginationBehavior.eNone;
			}
			String responseText = this.getSempV2(resource, paging);
			values = this.getDataObjectsElement(responseText, columnName);
		} catch (SempException | IOException e) {
			throw new SempException(e);
		} 
		return values;

	}
	public int getClientCount(String vpn) throws SempException {
//		/curl -X GET -u mikeTest-admin:8p1sqblhq9feuj3n28mkbdtnn4  https://mr-connection-lt30wl00r51.messaging.solace.cloud:943/SEMP/v2/monitor/msgVpns/mikeTest/clients?select=clientId&count=1
		String url = fullUrl.replace("config", "monitor");
		url += "/msgVpns/{msgVpnName}/clients?select=clientId&count=1";
		url = url.replace("{msgVpnName}", vpn);

		int clientCount = getCollectionCountAbsoluteUrl(url);
		return clientCount;
	}
	
	public int getCollectionCountRelativeUrl(String resource) throws SempException {
		String urlToUse = this.fullUrl + resource;
		return getCollectionCountAbsoluteUrl(urlToUse);
	}
	public int getCollectionCountAbsoluteUrl(String url) throws SempException {
		int count = -1;
		try {
			String responseText = HttpClient.fetch(url, user, pw);
			count = this.getMetaCount(responseText);
		} catch (IOException e) {
			throw new SempException(e);
		}
		return count;
	}
	

	@SuppressWarnings("unused")
	private void reflectiveLoadShort(Object obj, JSONObject container, Field fld) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		//Field fld = obj.getClass().getDeclaredField(fieldName);
		String fieldName = fld.getName();
		if (container.has(fieldName)) {
			fld.setAccessible(true);
			int value = (int) container.getInt(fieldName);
		    fld.setInt(obj, value);
		}
	}
}
