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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;

/**
 * Class intended to be used by clients who wish to invoke a forked Maven process from their applications
 *
 * @deprecated This class is deprecated. Use
 *             <a href="https://github.com/apache/maven-executor">maven-executor</a>
 *             ForkedExecutor or EmbeddedExecutor instead.
 *             See the <a href="https://maven.apache.org/shared/maven-invoker/migration.html">Migration Guide</a>
 *             for details.
 * @author jdcasey
 * @see <a href="https://github.com/apache/maven-invoker/issues/164">Issue #164</a>
 */
@Deprecated
@Named
@Singleton
public class DefaultInvoker implements Invoker {
    /** Constant <code>ROLE_HINT="default"</code> */
    public static final String ROLE_HINT = "default";

    private static final InvokerLogger DEFAULT_LOGGER = new SystemOutLogger();

    private static final InvocationOutputHandler DEFAULT_OUTPUT_HANDLER = new SystemOutHandler();

    private File localRepositoryDirectory;

    private InvokerLogger logger = DEFAULT_LOGGER;

    private File workingDirectory;

    private File mavenHome;

    private File mavenExecutable;

    private InvocationOutputHandler outputHandler = DEFAULT_OUTPUT_HANDLER;

    private InputStream inputStream;

    private InvocationOutputHandler errorHandler = DEFAULT_OUTPUT_HANDLER;

    /** {@inheritDoc} */
    public InvocationResult execute(InvocationRequest request) throws MavenInvocationException {
        MavenCommandLineBuilder cliBuilder = new MavenCommandLineBuilder() {
            @Override
            protected Commandline createCommandline() {
                return new ProcessTrackingCommandline();
            }
        };

        if (logger != null) {
            cliBuilder.setLogger(logger);
        }

        if (localRepositoryDirectory != null) {
            cliBuilder.setLocalRepositoryDirectory(localRepositoryDirectory);
        }

        if (mavenHome != null) {
            cliBuilder.setMavenHome(mavenHome);
        }

        if (mavenExecutable != null) {
            cliBuilder.setMavenExecutable(mavenExecutable);
        }

        if (workingDirectory != null) {
            cliBuilder.setBaseDirectory(workingDirectory);
        }

        ProcessTrackingCommandline cli;

        try {
            cli = (ProcessTrackingCommandline) cliBuilder.build(request);
        } catch (CommandLineConfigurationException e) {
            throw new MavenInvocationException("Error configuring command line", e);
        }

        DefaultInvocationResult result = new DefaultInvocationResult();

        try {
            int exitCode = executeCommandLine(cli, request, request.getTimeoutInSeconds());

            result.setExitCode(exitCode);
        } catch (CommandLineException e) {
            result.setExecutionException(e);
        }

        return result;
    }

    private int executeCommandLine(ProcessTrackingCommandline cli, InvocationRequest request, int timeoutInSeconds)
            throws CommandLineException {
        InputStream inputStream = request.getInputStream(this.inputStream);
        InvocationOutputHandler outputHandler = request.getOutputHandler(this.outputHandler);
        InvocationOutputHandler errorHandler = request.getErrorHandler(this.errorHandler);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Executing: " + cli);
        }

        if (request.isBatchMode()) {
            if (inputStream != null) {
                getLogger().info("Executing in batch mode. The configured input stream will be ignored.");
            }
            inputStream = null;
        } else if (inputStream == null) {
            getLogger()
                    .warn("Maven will be executed in interactive mode"
                            + ", but no input stream has been configured for this MavenInvoker instance.");
        }

        // the callback runs after a timeout while the Maven process is still alive, before
        // CommandLineUtils destroys it; that is the moment to take its descendants with it
        return CommandLineUtils.executeCommandLine(
                cli, inputStream, outputHandler, errorHandler, timeoutInSeconds, cli::destroyDescendantsIfAlive);
    }

    /**
     * A command line that remembers the process it starts, so that a build which ran into its timeout can be
     * stopped together with the processes it forked (Surefire, for instance), not just the launcher script.
     */
    static class ProcessTrackingCommandline extends Commandline {
        private volatile Process process;

        @Override
        public Process execute() throws CommandLineException {
            process = super.execute();
            return process;
        }

        void destroyDescendantsIfAlive() {
            Process p = process;
            if (p != null && p.isAlive()) {
                destroyDescendants(p);
            }
        }
    }

    /**
     * Forcibly destroys every descendant of the process, through {@code ProcessHandle} where the runtime is
     * Java 9 or later; on Java 8 only the process itself can be reached and its children are left as they are.
     */
    static void destroyDescendants(Process process) {
        try {
            Method toHandle = Process.class.getMethod("toHandle");
            Object handle = toHandle.invoke(process);
            Class<?> processHandle = Class.forName("java.lang.ProcessHandle");
            Method destroyForcibly = processHandle.getMethod("destroyForcibly");
            Stream<?> descendants =
                    (Stream<?>) processHandle.getMethod("descendants").invoke(handle);
            descendants.forEach(descendant -> {
                try {
                    destroyForcibly.invoke(descendant);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // the process may be gone already; nothing else to do for it
                }
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Java 8: no ProcessHandle
        }
    }

    /**
     * <p>Getter for the field <code>localRepositoryDirectory</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getLocalRepositoryDirectory() {
        return localRepositoryDirectory;
    }

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link org.apache.maven.shared.invoker.InvokerLogger} object.
     */
    public InvokerLogger getLogger() {
        return logger;
    }

    /** {@inheritDoc} */
    public Invoker setLocalRepositoryDirectory(File localRepositoryDirectory) {
        this.localRepositoryDirectory = localRepositoryDirectory;
        return this;
    }

    /** {@inheritDoc} */
    public Invoker setLogger(InvokerLogger logger) {
        this.logger = (logger != null) ? logger : DEFAULT_LOGGER;
        return this;
    }

    /**
     * <p>Getter for the field <code>workingDirectory</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /** {@inheritDoc} */
    public Invoker setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * <p>Getter for the field <code>mavenHome</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getMavenHome() {
        return mavenHome;
    }

    /** {@inheritDoc} */
    public Invoker setMavenHome(File mavenHome) {
        this.mavenHome = mavenHome;

        return this;
    }

    /**
     * <p>Getter for the field <code>mavenExecutable</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getMavenExecutable() {
        return mavenExecutable;
    }

    /** {@inheritDoc} */
    public Invoker setMavenExecutable(File mavenExecutable) {
        this.mavenExecutable = mavenExecutable;
        return this;
    }

    /** {@inheritDoc} */
    public Invoker setErrorHandler(InvocationOutputHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /** {@inheritDoc} */
    public Invoker setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /** {@inheritDoc} */
    public Invoker setOutputHandler(InvocationOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
        return this;
    }
}
