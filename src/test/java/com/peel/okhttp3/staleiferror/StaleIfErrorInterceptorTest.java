/*
 * Copyright (C) 2018 Peel Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peel.okhttp3.staleiferror;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import static okhttp3.tls.internal.TlsUtil.localhost;

/**
 * 
 * @author swaroop
 */
public class StaleIfErrorInterceptorTest {

	@Rule
	public final MockWebServer server = new MockWebServer();

	private HandshakeCertificates handshakeCertificates = localhost();

	private OkHttpClient client;
	private OkHttpClient clientWith4SecMaxStale;

	private HttpUrl url;

	@Before
	public void setUp() {
		client = new OkHttpClient.Builder()
				.addInterceptor(new StaleIfErrorInterceptor())
				.sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
				.cache(new Cache(new File("./caches"), (long) (1024 * 1024 * 10)))
				.connectTimeout(2, TimeUnit.SECONDS)
				.build();
		
		clientWith4SecMaxStale = new OkHttpClient.Builder()
				.addInterceptor(new StaleIfErrorInterceptor(4, TimeUnit.SECONDS))
				.sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager())
				.cache(new Cache(new File("./caches"), (long) (1024 * 1024 * 10)))
				.connectTimeout(2, TimeUnit.SECONDS)
				.build();

		url = server.url("/");
	}

	private Request.Builder request() {
		return new Request.Builder().url(url);
	}

	@Test
	public void testCachingWithMaxAge() {
		//setup a server with mock response and max age 2 sec
		MockResponse mockResponse = new MockResponse().setResponseCode(200)
				.setBody("Hello from mock server")
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=2");
		MockResponse mock404Response = new MockResponse().setResponseCode(404)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=2");
		server.enqueue(mockResponse);
		server.enqueue(mock404Response);
		server.enqueue(mockResponse);
		
		Request request = request().get().build();
		Response response = null;
		try {
			//make first call and cache the response
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNotNull(response.networkResponse());
			assertEquals(200, response.code());
			response.body().close();
			
			//get response from the local cache.
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			response.body().close();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//get response from the local cache 
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			response.body().close();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//get response from the stale cache
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			response.body().close();
			
			//bring back the server here
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNotNull(response.networkResponse());
			response.body().close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (response != null) {
				response.body().close();
			}
		}

	}
	
	@Test
	public void testIfCacheRefreshWithNewResponse() {
		// setup a server with mock response and max age 2 sec
		String responseOne = "Hello from mock server";
		String responseTwo = "Hello again from mock server";
		MockResponse mockResponse = new MockResponse()
				.setResponseCode(200)
				.setBody(responseOne)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=1");
		MockResponse mock404Response = new MockResponse()
				.setResponseCode(404)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=2");
		MockResponse mockResponse2 = new MockResponse()
				.setResponseCode(200)
				.setBody(responseTwo)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=2");
		server.enqueue(mockResponse);
		server.enqueue(mockResponse2);
		server.enqueue(mock404Response);
		
		Request request = request().get().build();
		Response response = null;
		try {
			//make first call and cache the response
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNotNull(response.networkResponse());
			assertEquals(200, response.code());
			assertEquals(responseOne, new String(response.body().bytes()));
			response.body().close();
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNotNull(response.networkResponse());
			assertEquals(200, response.code());
			assertEquals(responseTwo, new String(response.body().bytes()));
			response.body().close();
			
			//cached 2 response.
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			String actual = new String(response.body().bytes());
			assertEquals(responseTwo, actual);
			response.body().close();
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//cached 2 response.
			response = client.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			actual = new String(response.body().bytes());
			assertEquals(responseTwo, actual);
			response.body().close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
	}
	
	@Test
	public void testStaleCacheExpired() {
		
		String responseOne = "Hello from mock server";
		MockResponse mockResponse = new MockResponse()
				.setResponseCode(200)
				.setBody(responseOne)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=1");
		MockResponse mock404Response = new MockResponse()
				.setResponseCode(404)
				.addHeader("Content-Type: text/html; charset=utf-8")
				.addHeader("Cache-Control", "public, max-age=2");
		
		server.enqueue(mockResponse);
		server.enqueue(mock404Response);
		
		Request request = request().get().build();
		Response response = null;
		try {
			//make first call and cache the response
			response = clientWith4SecMaxStale.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNotNull(response.networkResponse());
			assertEquals(200, response.code());
			assertEquals(responseOne, new String(response.body().bytes()));
			response.body().close();
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			response = clientWith4SecMaxStale.newCall(request).execute();
			assertNotNull(response);
			assertNotNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(200, response.code());
			
			response.body().close();
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//stale is also expired here
			response = clientWith4SecMaxStale.newCall(request).execute();
			assertNotNull(response);
			assertNull(response.cacheResponse());
			assertNull(response.networkResponse());
			assertEquals(504, response.code());
			response.body().close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (response != null) {
				response.body().close();
			}
		}
		
	}

	@After
	public void tearDown() {
		File cachesDir = new File("./caches");
		if (cachesDir.exists() && cachesDir.isDirectory()) {
			for (File file : cachesDir.listFiles()) {
				file.delete();
			}
		}
		cachesDir.delete();
		
	}

}
