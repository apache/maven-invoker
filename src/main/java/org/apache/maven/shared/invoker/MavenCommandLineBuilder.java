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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.shared.invoker.InvocationRequest.CheckSumPolicy;
import org.apache.maven.shared.invoker.InvocationRequest.ReactorFailureBehavior;
import org.apache.maven.shared.utils.Os;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;

/**
 * <p>MavenCommandLineBuilder class.</p>
 */
public class MavenCommandLineBuilder
{

    private static final InvokerLogger DEFAULT_LOGGER = new SystemOutLogger();

    private InvokerLogger logger = DEFAULT_LOGGER;

    private File baseDirectory;

    private File localRepositoryDirectory;

    private File mavenHome;

    private File mavenExecutable;

    /**
     * <p>build.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @return a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     * @throws org.apache.maven.shared.invoker.CommandLineConfigurationException if any.
     */
    public Commandline build( InvocationRequest request )
        throws CommandLineConfigurationException
    {

        Commandline cli = new Commandline();

        setupMavenHome( request );

        // discover value for working directory
        setupBaseDirectory( request );
        cli.setWorkingDirectory( baseDirectory );

        checkRequiredState();

        setupMavenExecutable( request );
        cli.setExecutable( mavenExecutable.getAbsolutePath() );

        // handling for OS-level envars
        setShellEnvironment( request, cli );

        // interactive, offline, update-snapshots,
        // debug/show-errors, checksum policy
        setFlags( request, cli );

        // failure behavior and [eventually] forced-reactor
        // includes/excludes, etc.
        setReactorBehavior( request, cli );

        // local repository location
        setLocalRepository( request, cli );

        // pom-file handling
        setPomLocation( request, cli );

        setSettingsLocation( request, cli );

        setToolchainsLocation( request, cli );

        setProperties( request, cli );

        setProfiles( request, cli );

        setGoals( request, cli );

        setThreads( request, cli );

        setArgs( request, cli );

        return cli;
    }

    /**
     * <p>checkRequiredState.</p>
     */
    protected void checkRequiredState()
    {
        if ( logger == null )
        {
            throw new IllegalStateException( "A logger instance is required." );
        }
    }

    /**
     * <p>setSettingsLocation.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setSettingsLocation( InvocationRequest request, Commandline cli )
    {
        File userSettingsFile = request.getUserSettingsFile();

        if ( userSettingsFile != null )
        {
            try
            {
                userSettingsFile = userSettingsFile.getCanonicalFile();
            }
            catch ( IOException e )
            {
                logger.debug( "Failed to canonicalize user settings path: " + userSettingsFile.getAbsolutePath()
                    + ". Using as-is.", e );
            }

            cli.createArg().setValue( "-s" );
            cli.createArg().setValue( userSettingsFile.getPath() );
        }

        File globalSettingsFile = request.getGlobalSettingsFile();

        if ( globalSettingsFile != null )
        {
            try
            {
                globalSettingsFile = globalSettingsFile.getCanonicalFile();
            }
            catch ( IOException e )
            {
                logger.debug( "Failed to canonicalize global settings path: " + globalSettingsFile.getAbsolutePath()
                    + ". Using as-is.", e );
            }

            cli.createArg().setValue( "-gs" );
            cli.createArg().setValue( globalSettingsFile.getPath() );
        }

    }

    /**
     * <p>setToolchainsLocation.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setToolchainsLocation( InvocationRequest request, Commandline cli )
    {
        File toolchainsFile = request.getToolchainsFile();

        if ( toolchainsFile != null )
        {
            try
            {
                toolchainsFile = toolchainsFile.getCanonicalFile();
            }
            catch ( IOException e )
            {
                logger.debug( "Failed to canonicalize toolchains path: " + toolchainsFile.getAbsolutePath()
                    + ". Using as-is.", e );
            }

            cli.createArg().setValue( "-t" );
            cli.createArg().setValue( toolchainsFile.getPath() );
        }
    }

    /**
     * <p>setShellEnvironment.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setShellEnvironment( InvocationRequest request, Commandline cli )
    {
        if ( request.isShellEnvironmentInherited() )
        {
            cli.addSystemEnvironment();
        }

        if ( request.getJavaHome() != null )
        {
            cli.addEnvironment( "JAVA_HOME", request.getJavaHome().getAbsolutePath() );
        }

        if ( request.getMavenOpts() != null )
        {
            cli.addEnvironment( "MAVEN_OPTS", request.getMavenOpts() );
        }

        for ( Map.Entry<String, String> entry : request.getShellEnvironments().entrySet() )
        {
            cli.addEnvironment( entry.getKey(), entry.getValue() );
        }
    }

    /**
     * <p>setProfiles.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setProfiles( InvocationRequest request, Commandline cli )
    {
        List<String> profiles = request.getProfiles();

        if ( ( profiles != null ) && !profiles.isEmpty() )
        {
            cli.createArg().setValue( "-P" );
            cli.createArg().setValue( StringUtils.join( profiles.iterator(), "," ) );
        }

    }

    /**
     * <p>setGoals.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     * @throws org.apache.maven.shared.invoker.CommandLineConfigurationException if any.
     */
    protected void setGoals( InvocationRequest request, Commandline cli ) throws CommandLineConfigurationException
    {
        List<String> goals = request.getGoals();

        if ( ( goals != null ) && !goals.isEmpty() )
        {
            try
            {
                cli.createArg().setLine( StringUtils.join( goals.iterator(), " " ) );
            }
            catch ( CommandLineException e )
            {
                throw new CommandLineConfigurationException( "Problem to set goals: " + e.getMessage(), e );
            }
        }
    }

    /**
     * <p>setProperties.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setProperties( InvocationRequest request, Commandline cli )
    {
        Properties properties = request.getProperties();

        if ( properties != null )
        {
            for ( Entry<Object, Object> entry : properties.entrySet() )
            {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                cli.createArg().setValue( "-D" );
                cli.createArg().setValue( key + '=' + value );
            }
        }
    }

    /**
     * <p>setPomLocation.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setPomLocation( InvocationRequest request, Commandline cli )
    {
        File pom = request.getPomFile();
        String pomFilename = request.getPomFileName();

        if ( pom == null )
        {
            if ( pomFilename != null )
            {
                pom = new File( baseDirectory, pomFilename );
            }
            else
            {
                pom = new File( baseDirectory, "pom.xml" );
            }
        }

        try
        {
            pom = pom.getCanonicalFile();
        }
        catch ( IOException e )
        {
            logger.debug( "Failed to canonicalize the POM path: " + pom + ". Using as-is.", e );
        }

        if ( pom.getParentFile().equals( baseDirectory ) )
        {
            // pom in project workspace
            if ( !"pom.xml".equals( pom.getName() ) )
            {
                logger.debug( "Specified POM file is not named 'pom.xml'. "
                    + "Using the '-f' command-line option to accommodate non-standard filename..." );

                cli.createArg().setValue( "-f" );
                cli.createArg().setValue( pom.getName() );
            }
        }
        else
        {
            cli.createArg().setValue( "-f" );
            cli.createArg().setValue( pom.getPath() );
        }
    }

    void setupBaseDirectory( InvocationRequest request )
    {
        File baseDirectoryFromRequest = null;
        if ( request.getBaseDirectory() != null )
        {
            baseDirectoryFromRequest = request.getBaseDirectory();
        }
        else
        {
            File pomFile = request.getPomFile();
            if ( pomFile != null )
            {
                baseDirectoryFromRequest = pomFile.getParentFile();
            }
        }

        if ( baseDirectoryFromRequest != null )
        {
            baseDirectory = baseDirectoryFromRequest;
        }

        if ( baseDirectory == null )
        {
            baseDirectory = new File( System.getProperty( "user.dir" ) );
        }
        else if ( baseDirectory.isFile() )
        {
            logger.warn( "Specified base directory (" + baseDirectory + ") is a file."
                + " Using its parent directory..." );

            baseDirectory = baseDirectory.getParentFile();
        }

        try
        {
            baseDirectory = baseDirectory.getCanonicalFile();
        }
        catch ( IOException e )
        {
            logger.debug( "Failed to canonicalize base directory: " + baseDirectory + ". Using as-is.", e );
        }
    }

    /**
     * <p>setLocalRepository.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setLocalRepository( InvocationRequest request, Commandline cli )
    {
        File localRepositoryDirectory = request.getLocalRepositoryDirectory( this.localRepositoryDirectory );

        if ( localRepositoryDirectory != null )
        {
            try
            {
                localRepositoryDirectory = localRepositoryDirectory.getCanonicalFile();
            }
            catch ( IOException e )
            {
                logger.debug( "Failed to canonicalize local repository directory: " + localRepositoryDirectory
                    + ". Using as-is.", e );
            }

            if ( !localRepositoryDirectory.isDirectory() )
            {
                throw new IllegalArgumentException( "Local repository location: '" + localRepositoryDirectory
                    + "' is NOT a directory." );
            }

            cli.createArg().setValue( "-D" );
            cli.createArg().setValue( "maven.repo.local=" + localRepositoryDirectory.getPath() );
        }
    }

    /**
     * <p>setReactorBehavior.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setReactorBehavior( InvocationRequest request, Commandline cli )
    {
        // NOTE: The default is "fail-fast"
        ReactorFailureBehavior failureBehavior = request.getReactorFailureBehavior();

        if ( failureBehavior != null )
        {
            if ( ReactorFailureBehavior.FailAtEnd.equals( failureBehavior ) )
            {
                cli.createArg().setValue( "-" + ReactorFailureBehavior.FailAtEnd.getShortOption() );
            }
            else if ( ReactorFailureBehavior.FailNever.equals( failureBehavior ) )
            {
                cli.createArg().setValue( "-" + ReactorFailureBehavior.FailNever.getShortOption() );
            }

        }

        if ( StringUtils.isNotEmpty( request.getResumeFrom() ) )
        {
            cli.createArg().setValue( "-rf" );
            cli.createArg().setValue( request.getResumeFrom() );
        }

        List<String> projectList = request.getProjects();
        if ( projectList != null )
        {
            cli.createArg().setValue( "-pl" );
            cli.createArg().setValue( StringUtils.join( projectList.iterator(), "," ) );

            if ( request.isAlsoMake() )
            {
                cli.createArg().setValue( "-am" );
            }

            if ( request.isAlsoMakeDependents() )
            {
                cli.createArg().setValue( "-amd" );
            }
        }
    }

    /**
     * <p>setFlags.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setFlags( InvocationRequest request, Commandline cli )
    {
        if ( request.isBatchMode() )
        {
            cli.createArg().setValue( "-B" );
        }

        if ( request.isOffline() )
        {
            cli.createArg().setValue( "-o" );
        }

        if ( request.isUpdateSnapshots() )
        {
            cli.createArg().setValue( "-U" );
        }

        if ( !request.isRecursive() )
        {
            cli.createArg().setValue( "-N" );
        }

        if ( request.isDebug() )
        {
            cli.createArg().setValue( "-X" );
        }
        // this is superseded by -X, if it exists.
        else if ( request.isShowErrors() )
        {
            cli.createArg().setValue( "-e" );
        }

        CheckSumPolicy checksumPolicy = request.getGlobalChecksumPolicy();
        if ( CheckSumPolicy.Fail.equals( checksumPolicy ) )
        {
            cli.createArg().setValue( "-C" );
        }
        else if ( CheckSumPolicy.Warn.equals( checksumPolicy ) )
        {
            cli.createArg().setValue( "-c" );
        }

        if ( request.isNonPluginUpdates() )
        {
            cli.createArg().setValue( "-npu" );
        }

        if ( request.isShowVersion() )
        {
            cli.createArg().setValue( "-V" );
        }

        if ( request.getBuilder() != null )
        {
            cli.createArg().setValue( "-b" );
            cli.createArg().setValue( request.getBuilder() );
        }

        if ( request.isQuiet() )
        {
            cli.createArg().setValue( "-q" );
        }

        if ( request.isNoTransferProgress() )
        {
            cli.createArg().setValue( "-ntp" );
        }
    }

    /**
     * <p>setThreads.</p>
     *
     * @param request a {@link org.apache.maven.shared.invoker.InvocationRequest} object.
     * @param cli a {@link org.apache.maven.shared.utils.cli.Commandline} object.
     */
    protected void setThreads( InvocationRequest request, Commandline cli )
    {
        String threads = request.getThreads();
        if ( StringUtils.isNotEmpty( threads ) )
        {
            cli.createArg().setValue( "-T" );
            cli.createArg().setValue( threads );
        }

    }

    private void setArgs( InvocationRequest request, Commandline cli )
    {
        for ( String arg : request.getArgs() )
        {
            cli.createArg().setValue( arg );
        }
    }

    private void setupMavenHome( InvocationRequest request )
    {
        if ( request.getMavenHome() != null )
        {
            mavenHome = request.getMavenHome();
        }
        else if ( System.getProperty( "maven.home" ) != null )
        {
            mavenHome = new File( System.getProperty( "maven.home" ) );
        }

        if ( mavenHome != null && !mavenHome.isDirectory() )
        {
            File binDir = mavenHome.getParentFile();
            if ( binDir != null && "bin".equals( binDir.getName() ) )
            {
                // ah, they specified the mvn
                // executable instead...
                mavenHome = binDir.getParentFile();
            }
        }

        if ( mavenHome != null && !mavenHome.isDirectory() )
        {
            throw new IllegalStateException( "Maven home is set to: '" + mavenHome + "' which is not a directory" );
        }

        logger.debug( "Using maven.home of: '" + mavenHome + "'." );
    }


    /**
     * <p>setupMavenExecutable.</p>
     *
     * @param request a Invoker request
     * @throws org.apache.maven.shared.invoker.CommandLineConfigurationException if any.
     */
    protected void setupMavenExecutable( InvocationRequest request )
        throws CommandLineConfigurationException
    {
        if ( request.getMavenExecutable() != null )
        {
            mavenExecutable = request.getMavenExecutable();
        }

        if ( mavenExecutable == null || !mavenExecutable.isAbsolute() )
        {
            String executable;
            if ( mavenExecutable != null )
            {
                executable = mavenExecutable.getPath();
            }
            else
            {
                executable = "mvn";
            }

            // firs look in project directory
            mavenExecutable = detectMavenExecutablePerOs( baseDirectory, executable );
            if ( mavenExecutable == null )
            {
                // next maven home
                mavenExecutable = detectMavenExecutablePerOs( mavenHome, "/bin/" + executable );
            }

            if ( mavenExecutable != null )
            {
                try
                {
                    mavenExecutable = mavenExecutable.getCanonicalFile();
                }
                catch ( IOException e )
                {
                    logger.debug( "Failed to canonicalize maven executable: '" + mavenExecutable
                        + "'. Using as-is.", e );
                }
            }
            else
            {
                throw new CommandLineConfigurationException(
                    "Maven executable: '" + executable + "'"
                        + " not found at project dir: '" + baseDirectory + "' nor maven home: '" + mavenHome + "'" );
            }
        }
    }

    private File detectMavenExecutablePerOs( File baseDirectory, String executable )
    {
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            File executableFile = new File( baseDirectory, executable + ".cmd" );
            if ( executableFile.isFile() )
            {
                return executableFile;
            }

            executableFile = new File( baseDirectory, executable + ".bat" );
            if ( executableFile.isFile() )
            {
                return executableFile;
            }
        }

        File executableFile = new File( baseDirectory, executable );
        if ( executableFile.isFile() )
        {
            return executableFile;
        }
        return null;
    }

    /**
     * <p>Getter for the field <code>localRepositoryDirectory</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getLocalRepositoryDirectory()
    {
        return localRepositoryDirectory;
    }

    /**
     * <p>Setter for the field <code>localRepositoryDirectory</code>.</p>
     *
     * @param localRepositoryDirectory a {@link java.io.File} object.
     */
    public void setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link org.apache.maven.shared.invoker.InvokerLogger} object.
     */
    public InvokerLogger getLogger()
    {
        return logger;
    }

    /**
     * <p>Setter for the field <code>logger</code>.</p>
     *
     * @param logger a {@link org.apache.maven.shared.invoker.InvokerLogger} object.
     */
    public void setLogger( InvokerLogger logger )
    {
        this.logger = logger;
    }

    /**
     * <p>Getter for the field <code>mavenHome</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getMavenHome()
    {
        return mavenHome;
    }

    /**
     * <p>Setter for the field <code>mavenHome</code>.</p>
     *
     * @param mavenHome a {@link java.io.File} object.
     */
    public void setMavenHome( File mavenHome )
    {
        this.mavenHome = mavenHome;
    }

    /**
     * <p>Getter for the field <code>baseDirectory</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getBaseDirectory()
    {
        return baseDirectory;
    }

    /**
     * <p>Setter for the field <code>baseDirectory</code>.</p>
     *
     * @param baseDirectory a {@link java.io.File} object.
     */
    public void setBaseDirectory( File baseDirectory )
    {
        this.baseDirectory = baseDirectory;
    }

    /**
     * {@code mavenExecutable} can either be relative to ${maven.home}/bin/ or absolute
     *
     * @param mavenExecutable the executable
     */
    public void setMavenExecutable( File mavenExecutable )
    {
        this.mavenExecutable = mavenExecutable;
    }

    /**
     * <p>Getter for the field <code>mavenExecutable</code>.</p>
     *
     * @return a {@link java.io.File} object.
     */
    public File getMavenExecutable()
    {
        return mavenExecutable;
    }

}
