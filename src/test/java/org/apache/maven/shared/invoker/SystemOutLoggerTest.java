/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.invoker;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemOutLoggerTest {

    private static final Throwable EXCEPTION =
            new MalformedURLException("This is meant to happen. It's part of the test.");

    private static final String MESSAGE = "This is a test message.";

    @Test
    void debugWithMessageOnly() {
        logTestStart();
        new SystemOutLogger().debug(MESSAGE);
    }

    @Test
    void debugWithMessageAndError() {
        logTestStart();
        new SystemOutLogger().debug(MESSAGE, EXCEPTION);
    }

    @Test
    void debugWithNullMessageAndNoError() {
        logTestStart();
        new SystemOutLogger().debug(null);
    }

    @Test
    void debugWithNullMessageError() {
        logTestStart();
        new SystemOutLogger().debug(null, EXCEPTION);
    }

    @Test
    void debugWithMessageNullError() {
        logTestStart();
        new SystemOutLogger().debug(MESSAGE, null);
    }

    @Test
    void infoWithMessageOnly() {
        logTestStart();
        new SystemOutLogger().info(MESSAGE);
    }

    @Test
    void infoWithMessageAndError() {
        logTestStart();
        new SystemOutLogger().info(MESSAGE, EXCEPTION);
    }

    @Test
    void infoWithNullMessageAndNoError() {
        logTestStart();
        new SystemOutLogger().info(null);
    }

    @Test
    void infoWithNullMessageError() {
        logTestStart();
        new SystemOutLogger().info(null, EXCEPTION);
    }

    @Test
    void infoWithMessageNullError() {
        logTestStart();
        new SystemOutLogger().info(MESSAGE, null);
    }

    @Test
    void warnWithMessageOnly() {
        logTestStart();
        new SystemOutLogger().warn(MESSAGE);
    }

    @Test
    void warnWithMessageAndError() {
        logTestStart();
        new SystemOutLogger().warn(MESSAGE, EXCEPTION);
    }

    @Test
    void warnWithNullMessageAndNoError() {
        logTestStart();
        new SystemOutLogger().warn(null);
    }

    @Test
    void warnWithNullMessageError() {
        logTestStart();
        new SystemOutLogger().warn(null, EXCEPTION);
    }

    @Test
    void warnWithMessageNullError() {
        logTestStart();
        new SystemOutLogger().warn(MESSAGE, null);
    }

    @Test
    void errorWithMessageOnly() {
        logTestStart();
        new SystemOutLogger().error(MESSAGE);
    }

    @Test
    void errorWithMessageAndError() {
        logTestStart();
        new SystemOutLogger().error(MESSAGE, EXCEPTION);
    }

    @Test
    void errorWithNullMessageAndNoError() {
        logTestStart();
        new SystemOutLogger().error(null);
    }

    @Test
    void errorWithNullMessageError() {
        logTestStart();
        new SystemOutLogger().error(null, EXCEPTION);
    }

    @Test
    void errorWithMessageNullError() {
        logTestStart();
        new SystemOutLogger().error(MESSAGE, null);
    }

    @Test
    void fatalErrorWithMessageOnly() {
        logTestStart();
        new SystemOutLogger().fatalError(MESSAGE);
    }

    @Test
    void fatalErrorWithMessageAndError() {
        logTestStart();
        new SystemOutLogger().fatalError(MESSAGE, EXCEPTION);
    }

    @Test
    void fatalErrorWithNullMessageAndNoError() {
        logTestStart();
        new SystemOutLogger().fatalError(null);
    }

    @Test
    void fatalErrorWithNullMessageError() {
        logTestStart();
        new SystemOutLogger().fatalError(null, EXCEPTION);
    }

    @Test
    void fatalErrorWithMessageNullError() {
        logTestStart();
        new SystemOutLogger().fatalError(MESSAGE, null);
    }

    @Test
    void defaultThresholdInfo() {
        assertEquals(InvokerLogger.INFO, new SystemOutLogger().getThreshold());
    }

    @Test
    void thresholdDebug() {
        InvokerLogger logger = new SystemOutLogger();
        logger.setThreshold(InvokerLogger.DEBUG);
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isFatalErrorEnabled());
    }

    @Test
    void thresholdInfo() {
        InvokerLogger logger = new SystemOutLogger();
        logger.setThreshold(InvokerLogger.INFO);
        assertFalse(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isFatalErrorEnabled());
    }

    @Test
    void thresholdWarn() {
        InvokerLogger logger = new SystemOutLogger();
        logger.setThreshold(InvokerLogger.WARN);
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isFatalErrorEnabled());
    }

    @Test
    void thresholdError() {
        InvokerLogger logger = new SystemOutLogger();
        logger.setThreshold(InvokerLogger.ERROR);
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertFalse(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        assertTrue(logger.isFatalErrorEnabled());
    }

    @Test
    void thresholdFatal() {
        InvokerLogger logger = new SystemOutLogger();
        logger.setThreshold(InvokerLogger.FATAL);
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertFalse(logger.isWarnEnabled());
        assertFalse(logger.isErrorEnabled());
        assertTrue(logger.isFatalErrorEnabled());
    }

    // this is just a debugging helper for separating unit test output...
    private void logTestStart() {
        NullPointerException npe = new NullPointerException();
        StackTraceElement element = npe.getStackTrace()[1];

        System.out.println("Starting: " + element.getMethodName());
    }
}
