package com.solace.psg.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarImpl implements CookieJar {
	private static final Logger logger = LoggerFactory.getLogger(CookieJarImpl.class.getName());
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        cookieStore.put(host, cookies);
        
        logger.debug(host + "'s cookies = " + cookies.toString());
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String host = url.host();
        return cookieStore.getOrDefault(host, Collections.emptyList());
    }

}