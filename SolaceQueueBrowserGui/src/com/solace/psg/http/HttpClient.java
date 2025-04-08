package com.solace.psg.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.brokers.BrokerException;
import com.solace.psg.util.StackTracePrinter;

import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Simple wrapper to create a simplified interface to do an http fetch. Current
 * implementation is "Ok http", but it could be anything.
 *
 */
public class HttpClient {
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class.getName());
	private OkHttpClient client = null;

	public static final String XML = "application/text; charset=utf-8";
	public static final String JSON = "application/json";
	public static final int defaultWaitInMs = 10000;

	// Solace Cloud servicing calls ---------------------------------------------------------------------
	private OkHttpClient clientFactory(Map<String, String> header) {
		if (this.client == null) {
			this.client = new OkHttpClient.Builder().readTimeout(defaultWaitInMs, TimeUnit.MILLISECONDS)
				.writeTimeout(defaultWaitInMs, TimeUnit.MILLISECONDS).build();
		}
		return this.client;
	}
	public String deleteWithHeaders(String url, Map<String, String> header) throws IOException {
		OkHttpClient ok = this.clientFactory(header);
		Headers headerbuild = Headers.of(header);
		Request.Builder builder = new Request.Builder();
		Request request = builder.url(url).headers(headerbuild).delete().build();
		OkHttpClient client = ok.newBuilder().build();
		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	public String getWithHeaders(String url, Map<String, String> header) throws IOException {
		OkHttpClient ok = this.clientFactory(header);
		Headers headerbuild = Headers.of(header);
		Request.Builder builder = new Request.Builder();
		Request request = builder.url(url).headers(headerbuild).build();
		OkHttpClient client = ok.newBuilder().build();
		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	public String postWithHeaders(String url, Map<String, String> header, String body, String contentType)
			throws IOException, BrokerException {
		logger.info("POST to " + url);
		MediaType ty = MediaType.parse(contentType);
		RequestBody bodyObj = RequestBody.create(ty, body);

		OkHttpClient ok = this.clientFactory(header);
		Headers headerbuild = Headers.of(header);
		Request.Builder builder = new Request.Builder();
		Request request = builder.url(url).headers(headerbuild).post(bodyObj).build();
		OkHttpClient client = ok.newBuilder().build();
		Response response = client.newCall(request).execute();

		String txt = response.body().string();
		return txt;
	}
	public String patchWithHeaders(String url, Map<String, String> header, String body, String contentType)
			throws IOException, BrokerException {
		logger.info("POST to " + url);
		MediaType ty = MediaType.parse(contentType);
		RequestBody bodyObj = RequestBody.create(ty, body);

		OkHttpClient ok = this.clientFactory(header);
		Headers headerbuild = Headers.of(header);
		Request.Builder builder = new Request.Builder();
		Request request = builder.url(url).headers(headerbuild).patch(bodyObj).build();
		OkHttpClient client = ok.newBuilder().build();
		Response response = client.newCall(request).execute();

		String txt = response.body().string();
		return txt;
	}

	// Broker servicing calls ---------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------
	public static String fetch(String url, String username, String password) throws IOException {
		return get(url, username, password, defaultWaitInMs);
	}
	public static String get(String url, String username, String password, int nTimeoutMilliSeconds)
			throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		return doGet(httpClient, url, username, password);
	}

	public static String delete(String url, String username, String password) throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		return doDelete(httpClient, url, username, password);
	}

	public static String postWithBody(String url, String username, String password, String body, String contentType)
			throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		return doPost(httpClient, url, body, contentType, username, password);
	}
	public static String putWithBody(String url, String username, String password, String body, String contentType)
			throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		return doPut(httpClient, url, body, contentType, username, password);
	}

	public static String patchWithBody(String url, String username, String password, String body, String contentType)
			throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		// execute request
		return doPatch(httpClient, url, body, contentType, username, password);
	}

	public static String postWithBody(String url, String username, String password, String body,
			int nTimeoutMilliSeconds, String contentType) throws IOException {

		OkHttpClient httpClient = unauthenticatedClientFactory();
		// execute request
		return doPost(httpClient, url, body, contentType, username, password);
	}

	public static void flushAllConnections() {
		if (poolSingleton != null) {
			poolSingleton.evictAll();
		}
		if (unauthenticatedClient != null) {
			unauthenticatedClient = null;
		}
	}
	
	private static ConnectionPool poolSingleton = null;
	private static OkHttpClient unauthenticatedClient = null;
	
	private static ConnectionPool poolFactory() {
		if (poolSingleton == null) {
			poolSingleton = new ConnectionPool(12, 5, TimeUnit.MINUTES);
		}
		return poolSingleton;
	}
	private static OkHttpClient unauthenticatedClientFactory() {
		if (unauthenticatedClient == null) {
			unauthenticatedClient = createUnauthenticatedClient();
		}
		return unauthenticatedClient;
	}
	
	private static OkHttpClient createUnauthenticatedClient() {
		int nTimeoutMilliSeconds = defaultWaitInMs;
		final TrustManager[] trustAllCerts = new TrustManager[] { new TrustManagerImpl() };

		OkHttpClient httpClient = null;
		try {
			// Install the all-trusting trust manager
		    final SSLContext sslContext = SSLContext.getInstance("SSL");
		    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		    // Create an ssl socket factory with our all-trusting manager
		    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		    
			// build client with authentication information.
			OkHttpClient.Builder bob = new OkHttpClient.Builder();
			//Authenticator basic = new BasicAuthenticator(username, password);
			//bob.authenticator(basic);
			bob.connectTimeout(nTimeoutMilliSeconds, TimeUnit.MILLISECONDS);
			bob.writeTimeout(nTimeoutMilliSeconds, TimeUnit.MILLISECONDS);
			bob.readTimeout(nTimeoutMilliSeconds, TimeUnit.MILLISECONDS);

			ConnectionPool pool = poolFactory();
			bob.connectionPool(pool);
			bob.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
	
			CookieJarImpl jar = new CookieJarImpl();
			bob.cookieJar(jar);
			
			HostnameVerifier letItPass = new TolerantHostnameVerifier();
			bob.hostnameVerifier(letItPass);

			httpClient = bob.build();
		}
		catch (Exception e) {
			logger.debug(StackTracePrinter.toString(e));
		}
		
		return httpClient;
	}
	/*
	private static String hostAndPostPrimaryKey(String url) {
		url = url.replace("http://", "");
		url = url.replace("https://", "");
		String[] parts = url.split("/");
		return parts[0];
	}
	private static Map<String, OkHttpClient> hostClientMap = new HashMap<String, OkHttpClient> ();
	private static OkHttpClient getHttpClient(String url) {
		String primaryKey = hostAndPostPrimaryKey(url);
		String primaryKey = "onlyOne";
		if (hostClientMap.containsKey(primaryKey)) {
			
		}
	}*/
	
	private static String execute(Builder builder) throws IOException {
		ConnectionPool pool = poolFactory();
        logger.debug("OkHttp Connection Pool: " + pool.connectionCount() + " total connections, " + pool.idleConnectionCount() + " idle connections.");

		Request request = builder.build();
		OkHttpClient httpClient = unauthenticatedClientFactory();
		Response response = httpClient.newCall(request).execute();
		if (!response.isSuccessful()) {
			String result = response.body().string();
			response.close();
			if (result.length() > 0) {
				Exception e = new Exception(result);
				throw new IOException(e);
			} else {
				throw new IOException("Unexpected code " + response);
			}
		}
		return response.body().string();
	}
	private static Builder builderFactory(String anyURL, String username, String password) {
		Builder builder = new Request.Builder();
		builder.url(anyURL);
		builder.addHeader("Authorization", Credentials.basic(username, password));
		logger.debug("call on " + anyURL + " uses " + username + "/" + password);
		return builder;
	}
	private static String doDelete(OkHttpClient httpClient, String anyURL, String username, String password) throws IOException {
		logger.info("DELETE on " + anyURL);
		Builder builder = builderFactory(anyURL, username, password);
		builder.delete();
		return execute(builder);
	}

	private static String doPost(OkHttpClient httpClient, String anyURL, String body, String contentType, String username, String password)
			throws IOException {
		logger.info("POST to " + anyURL);

		//String host = hostAndPostPrimaryKey(anyURL);
		//logger.info("^^ that host is " + host);
		
		MediaType ty = MediaType.parse(contentType);
		RequestBody bodyObj = RequestBody.create(ty, body);

		Builder builder = builderFactory(anyURL, username, password);
		builder.post(bodyObj);
		return execute(builder);
	}

	private static String doPatch(OkHttpClient httpClient, String anyURL, String body, String contentType, String username, String password)
			throws IOException {
		logger.info("PATCH to " + anyURL);
		MediaType ty = MediaType.parse(contentType);
		RequestBody bodyObj = RequestBody.create(ty, body);

		Builder builder = builderFactory(anyURL, username, password);
		builder.patch(bodyObj);
		return execute(builder);
	}
	private static String doPut(OkHttpClient httpClient, String anyURL, String body, String contentType, String username, String password)
			throws IOException {
		logger.info("PUT to " + anyURL);
		MediaType ty = MediaType.parse(contentType);
		RequestBody bodyObj = RequestBody.create(ty, body);

		Builder builder = builderFactory(anyURL, username, password);
		builder.put(bodyObj);
		return execute(builder);
	}
	private static String doGet(OkHttpClient httpClient, String anyURL, String username, String password) throws IOException {
		logger.info("GET from " + anyURL);
		Builder builder = builderFactory(anyURL, username, password);
		builder.get();
		return execute(builder);
	}
}
