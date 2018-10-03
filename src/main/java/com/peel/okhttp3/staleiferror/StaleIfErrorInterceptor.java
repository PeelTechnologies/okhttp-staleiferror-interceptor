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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.CacheControl.Builder;

public class StaleIfErrorInterceptor implements Interceptor {
    // based on code from https://github.com/square/okhttp/issues/1083 by jvincek
	private static final int DEFULAT_MAX_STALE_DURATION = 28;
	private static final TimeUnit DEFULAT_MAX_STALE_TIMEUNIT = TimeUnit.DAYS;
	
	private final int staleDuration;
	private final TimeUnit staleDurationTimeUnit;
	
    public StaleIfErrorInterceptor(int staleDuration, TimeUnit staleDurationTimeUnit) {
		super();
		this.staleDuration = staleDuration;
		this.staleDurationTimeUnit = staleDurationTimeUnit;
	}
    
    public StaleIfErrorInterceptor() {
		super();
		this.staleDuration = -1;
		this.staleDurationTimeUnit = null;
	}
    
	private int getMaxStaleDuration() {
		return staleDuration != -1 ? staleDuration : DEFULAT_MAX_STALE_DURATION;
	}
	
	private TimeUnit getMaxStaleDurationTimeUnit() {
		return staleDurationTimeUnit != null ? staleDurationTimeUnit : DEFULAT_MAX_STALE_TIMEUNIT;
	}

	@Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response = null;
        Request request = chain.request();

        // first try the regular (network) request, guard with try-catch
        // so we can retry with force-cache below
        try {
            response = chain.proceed(request);

            // return the original response only if it succeeds
            if (response.isSuccessful()) {
                return response;
            }
        } catch (Exception e) {
            // original request error
        }

		if (response == null || !response.isSuccessful()) {
		 CacheControl cacheControl = new Builder()
				      .onlyIfCached()
				      .maxStale(getMaxStaleDuration(), getMaxStaleDurationTimeUnit())
				      .build();
			Request newRequest = request.newBuilder().cacheControl(cacheControl).build();
			try {
				response = chain.proceed(newRequest);
			} catch (Exception e) { // cache not available
				throw e;
			}
		}

        return response;
    }
}