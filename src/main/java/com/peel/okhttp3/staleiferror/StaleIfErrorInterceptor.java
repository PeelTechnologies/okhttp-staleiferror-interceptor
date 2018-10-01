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

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class StaleIfErrorInterceptor implements Interceptor {
    // based on code from https://github.com/square/okhttp/issues/1083 by jvincek

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
            Request newRequest = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
            try {
                response = chain.proceed(newRequest);
            } catch (Exception e) { // cache not available
                throw e;
            }
        }

        return response;
    }
}