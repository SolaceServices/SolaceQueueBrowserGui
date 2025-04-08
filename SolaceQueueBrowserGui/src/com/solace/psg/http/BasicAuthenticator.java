package com.solace.psg.http;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class BasicAuthenticator implements Authenticator {
	String username;
	String password;
	public BasicAuthenticator (String username, String password) {
		this.username = username;
		this.password = password;
	} 
	public Request authenticate(Route route, Response response) throws IOException {
		String credential = Credentials.basic(username, password);
		if (responseCount(response) >= 3) {
			return null;
		}
		return response.request().newBuilder().header("Authorization", credential).build();
	}
	private static int responseCount(Response response) {
		int result = 1;
		while ((response = response.priorResponse()) != null) {
			result++;
		}
		return result;
	}

}
