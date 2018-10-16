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

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * This class tests the precondition checks for {@code StaleIfErrorInterceptor} constructor.
 *
 * @author Inderjeet Singh
 */
public class PreconditionTest {

    @Test
    public void testValidTimeUnitsAllowed() throws Exception {
        new StaleIfErrorInterceptor(1, TimeUnit.HOURS);
        new StaleIfErrorInterceptor(2, TimeUnit.MINUTES);
        new StaleIfErrorInterceptor(1, TimeUnit.SECONDS);
        new StaleIfErrorInterceptor(1, TimeUnit.MICROSECONDS);
        new StaleIfErrorInterceptor(1, TimeUnit.MILLISECONDS);
        new StaleIfErrorInterceptor(1, TimeUnit.NANOSECONDS);
    }

    @Test
    public void testZeroStaleDurationNotAllowed() throws Exception {
        try {
            new StaleIfErrorInterceptor(0, TimeUnit.HOURS);
            fail();
        } catch (AssertionError expected) {}
    }

    @Test
    public void testNegativeStaleDurationNotAllowed() throws Exception {
        try {
            new StaleIfErrorInterceptor(-1, TimeUnit.HOURS);
            fail();
        } catch (AssertionError expected) {}
    }

    @Test
    public void testNullTimeUnitNotAllowed() throws Exception {
        new StaleIfErrorInterceptor(1, TimeUnit.HOURS); // allowed
        try {
            new StaleIfErrorInterceptor(1, null);
            fail();
        } catch (AssertionError expected) {}
    }
}
