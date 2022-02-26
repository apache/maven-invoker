package org.apache.maven.shared.invoker;

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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.utils.StringUtils;

/**
 * Specifies the parameters used to control a Maven invocation.
 *
 */
public class DefaultInvocationRequest
    implements InvocationRequest
{

    private File basedir;

    private boolean debug;

    private InvocationOutputHandler errorHandler;

    private ReactorFailureBehavior failureBehavior = ReactorFailureBehavior.FailFast;

    private List<String> goals;

    private InputStream inputStream;

    private boolean interactive;

    private File localRepository;

    private boolean offline;

    private boolean recursive = true;

    private InvocationOutputHandler outputHandler;

    private File pomFile;

    private Properties properties;

    private boolean showErrors;

    private boolean updateSnapshots;

    private boolean shellEnvironmentInherited = true;

    private File userSettings;

    private File globalSettings;

    private File toolchains;

    private File globalToolchains;

    private CheckSumPolicy globalChecksumPolicy;

    private String pomFilename;

    private File javaHome;

    private List<String> profiles;

    private boolean nonPluginUpdates;

    private Map<String, String> shellEnvironments;

    private String mavenOpts;

    private List<String> projects;

    private boolean alsoMake;

    private boolean alsoMakeDependents;

    private String resumeFrom;

    private boolean showVersion;

    private String threads;

    private String builderId;

    private int timeoutInSeconds = NO_TIMEOUT;

    private boolean quiet;

    private File mavenHome;

    private File mavenExecutable;

    private boolean noTransferProgress;

    private List<String> args = new ArrayList<>();

    /**
     * <p>getBaseDirectory.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getBaseDirectory()
    {
        return basedir;
    }

    /** {@inheritDoc} */
    public File getBaseDirectory( File defaultDirectory )
    {
        return basedir == null ? defaultDirectory : basedir;
    }

    /** {@inheritDoc} */
    public InvocationOutputHandler getErrorHandler( InvocationOutputHandler defaultHandler )
    {
        return errorHandler == null ? defaultHandler : errorHandler;
    }

    /**
     * <p>getReactorFailureBehavior.</p>
     *
     * @return a ReactorFailureBehavior object.
     */
    public ReactorFailureBehavior getReactorFailureBehavior()
    {
        return failureBehavior;
    }

    /**
     * <p>Getter for the field <code>goals</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getGoals()
    {
        return goals;
    }

    /** {@inheritDoc} */
    public InputStream getInputStream( InputStream defaultStream )
    {
        return inputStream == null ? defaultStream : inputStream;
    }

    /** {@inheritDoc} */
    public File getLocalRepositoryDirectory( File defaultDirectory )
    {
        return localRepository == null ? defaultDirectory : localRepository;
    }

    /** {@inheritDoc} */
    public InvocationOutputHandler getOutputHandler( InvocationOutputHandler defaultHandler )
    {
        return outputHandler == null ? defaultHandler : outputHandler;
    }

    /**
     * <p>Getter for the field <code>pomFile</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getPomFile()
    {
        return pomFile;
    }

    /**
     * <p>Getter for the field <code>properties</code>.</p>
     *
     * @return a {@link java.util.Properties} object.
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * <p>isDebug.</p>
     *
     * @return a boolean.
     */
    public boolean isDebug()
    {
        return debug;
    }

    /**
     * <p>isBatchMode.</p>
     *
     * @return a boolean.
     */
    public boolean isBatchMode()
    {
        return interactive;
    }

    /**
     * <p>isOffline.</p>
     *
     * @return a boolean.
     */
    public boolean isOffline()
    {
        return offline;
    }

    /**
     * <p>isShowErrors.</p>
     *
     * @return a boolean.
     */
    public boolean isShowErrors()
    {
        return showErrors;
    }

    /**
     * <p>isUpdateSnapshots.</p>
     *
     * @return a boolean.
     */
    public boolean isUpdateSnapshots()
    {
        return updateSnapshots;
    }

    /**
     * <p>isRecursive.</p>
     *
     * @return a boolean.
     */
    public boolean isRecursive()
    {
        return recursive;
    }

    /** {@inheritDoc} */
    public InvocationRequest setRecursive( boolean recursive )
    {
        this.recursive = recursive;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setBaseDirectory( File basedir )
    {
        this.basedir = basedir;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setDebug( boolean debug )
    {
        this.debug = debug;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setErrorHandler( InvocationOutputHandler errorHandler )
    {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * <p>setReactorFailureBehavior.</p>
     *
     * @param failureBehavior a ReactorFailureBehavior object.
     * @return a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     */
    public InvocationRequest setReactorFailureBehavior( ReactorFailureBehavior failureBehavior )
    {
        this.failureBehavior = failureBehavior;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setGoals( List<String> goals )
    {
        this.goals = goals;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setInputStream( InputStream inputStream )
    {
        this.inputStream = inputStream;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setBatchMode( boolean interactive )
    {
        this.interactive = interactive;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setLocalRepositoryDirectory( File localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setOffline( boolean offline )
    {
        this.offline = offline;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setOutputHandler( InvocationOutputHandler outputHandler )
    {
        this.outputHandler = outputHandler;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setPomFile( File pomFile )
    {
        this.pomFile = pomFile;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setProperties( Properties properties )
    {
        this.properties = properties;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setShowErrors( boolean showErrors )
    {
        this.showErrors = showErrors;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setUpdateSnapshots( boolean updateSnapshots )
    {
        this.updateSnapshots = updateSnapshots;
        return this;
    }

    /**
     * <p>isShellEnvironmentInherited.</p>
     *
     * @see MavenCommandLineBuilder#setShellEnvironment(InvocationRequest, Commandline)
     * @return a boolean.
     */
    public boolean isShellEnvironmentInherited()
    {
        return shellEnvironmentInherited;
    }

    /** {@inheritDoc} */
    public InvocationRequest setShellEnvironmentInherited( boolean shellEnvironmentInherited )
    {
        this.shellEnvironmentInherited = shellEnvironmentInherited;
        return this;
    }

    /**
     * <p>Getter for the field <code>javaHome</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getJavaHome()
    {
        return javaHome;
    }

    /** {@inheritDoc} */
    public InvocationRequest setJavaHome( File javaHome )
    {
        this.javaHome = javaHome;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.io.File} object.
     */
    public File getUserSettingsFile()
    {
        return userSettings;
    }

    /** {@inheritDoc} */
    public InvocationRequest setUserSettingsFile( File userSettings )
    {
        this.userSettings = userSettings;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.io.File} object.
     */
    public File getGlobalSettingsFile()
    {
        return globalSettings;
    }

    /** {@inheritDoc} */
    public InvocationRequest setGlobalSettingsFile( File globalSettings )
    {
        this.globalSettings = globalSettings;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.io.File} object.
     */
    public File getToolchainsFile()
    {
        return toolchains;
    }

    /** {@inheritDoc} */
    public InvocationRequest setToolchainsFile( File toolchains )
    {
        this.toolchains = toolchains;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.io.File} object.
     */
    public File getGlobalToolchainsFile()
    {
        return globalToolchains;
    }

    /** {@inheritDoc} */
    public InvocationRequest setGlobalToolchainsFile( File toolchains )
    {
        this.globalToolchains = toolchains;
        return this;
    }


    /**
     * {@inheritDoc}
     *
     * @return a CheckSumPolicy object.
     */
    public CheckSumPolicy getGlobalChecksumPolicy()
    {
        return globalChecksumPolicy;
    }

    /**
     * {@inheritDoc}
     *
     * @param globalChecksumPolicy a CheckSumPolicy object.
     * @return a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     */
    public InvocationRequest setGlobalChecksumPolicy( CheckSumPolicy globalChecksumPolicy )
    {
        this.globalChecksumPolicy = globalChecksumPolicy;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPomFileName()
    {
        return pomFilename;
    }


    @Override
    public InvocationRequest addArg( String arg )
    {
        if ( StringUtils.isNotBlank( arg ) )
        {
            args.add( arg );
        }
        return this;
    }

    public List<String> getArgs()
    {
        return args;
    }

    /** {@inheritDoc} */
    public InvocationRequest setPomFileName( String pomFilename )
    {
        this.pomFilename = pomFilename;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getProfiles()
    {
        return profiles;
    }

    /** {@inheritDoc} */
    public InvocationRequest setProfiles( List<String> profiles )
    {
        this.profiles = profiles;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a boolean.
     */
    public boolean isNonPluginUpdates()
    {
        return nonPluginUpdates;
    }

    /** {@inheritDoc} */
    public InvocationRequest setNonPluginUpdates( boolean nonPluginUpdates )
    {
        this.nonPluginUpdates = nonPluginUpdates;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest addShellEnvironment( String name, String value )
    {
        if ( this.shellEnvironments == null )
        {
            this.shellEnvironments = new HashMap<>();
        }
        this.shellEnvironments.put( name, value );
        return this;
    }

    /**
     * <p>Getter for the field <code>shellEnvironments</code>.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> getShellEnvironments()
    {
        return shellEnvironments == null ? Collections.<String, String>emptyMap() : shellEnvironments;
    }

    /**
     * <p>Getter for the field <code>mavenOpts</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMavenOpts()
    {
        return mavenOpts;
    }

    /** {@inheritDoc} */
    public InvocationRequest setMavenOpts( String mavenOpts )
    {
        this.mavenOpts = mavenOpts;
        return this;
    }

    /**
     * <p>isShowVersion.</p>
     *
     * @see org.apache.maven.shared.invoker.InvocationRequest#isShowVersion()
     * @return a boolean.
     */
    public boolean isShowVersion()
    {
        return this.showVersion;
    }

    /** {@inheritDoc} */
    public InvocationRequest setShowVersion( boolean showVersion )
    {
        this.showVersion = showVersion;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.lang.String} object.
     */
    public String getThreads()
    {
        return threads;
    }

    /** {@inheritDoc} */
    public InvocationRequest setThreads( String threads )
    {
        this.threads = threads;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getProjects()
    {
        return projects;
    }

    /** {@inheritDoc} */
    public InvocationRequest setProjects( List<String> projects )
    {
        this.projects = projects;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a boolean.
     */
    public boolean isAlsoMake()
    {
        return alsoMake;
    }

    /** {@inheritDoc} */
    public InvocationRequest setAlsoMake( boolean alsoMake )
    {
        this.alsoMake = alsoMake;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a boolean.
     */
    public boolean isAlsoMakeDependents()
    {
        return alsoMakeDependents;
    }

    /** {@inheritDoc} */
    public InvocationRequest setAlsoMakeDependents( boolean alsoMakeDependents )
    {
        this.alsoMakeDependents = alsoMakeDependents;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.lang.String} object.
     */
    public String getResumeFrom()
    {
        return resumeFrom;
    }

    /** {@inheritDoc} */
    public InvocationRequest setResumeFrom( String resumeFrom )
    {
        this.resumeFrom = resumeFrom;
        return this;
    }

    /** {@inheritDoc} */
    public InvocationRequest setBuilder( String id )
    {
        this.builderId = id;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBuilder()
    {
        return this.builderId;
    }


    /** {@inheritDoc} */
    @Override
    public int getTimeoutInSeconds()
    {
        return timeoutInSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public void setTimeoutInSeconds( int timeoutInSeconds )
    {
        this.timeoutInSeconds = timeoutInSeconds;
    }

    /**
     * {@inheritDoc}
     *
     * @return a boolean.
     * @since 3.1.0
     */
    public boolean isQuiet()
    {
        return quiet;
    }

    /** {@inheritDoc} */
    public InvocationRequest setQuiet( boolean quiet )
    {
        this.quiet = quiet;
        return this;
    }

    @Override
    public boolean isNoTransferProgress()
    {
        return noTransferProgress;
    }

    @Override
    public InvocationRequest setNoTransferProgress( boolean noTransferProgress )
    {
        this.noTransferProgress = noTransferProgress;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getMavenHome()
    {
        return mavenHome;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvocationRequest setMavenHome( File mavenHome )
    {
        this.mavenHome = mavenHome;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getMavenExecutable()
    {
        return mavenExecutable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvocationRequest setMavenExecutable( File mavenExecutable )
    {
        this.mavenExecutable = mavenExecutable;
        return this;
    }
}
