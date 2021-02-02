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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.utils.Os;
import org.apache.maven.shared.utils.cli.Commandline;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

public class MavenCommandLineBuilderTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Properties sysProps;
    private File lrd;
    private TestCommandLineBuilder tcb = new TestCommandLineBuilder();
    private Commandline cli = new Commandline();

    @Before
    public void setUp() throws IOException
    {
        sysProps = System.getProperties();
        Properties p = new Properties( sysProps );

        System.setProperties( p );
        
        lrd = temporaryFolder.newFile();

    }

    @After
    public void tearDown()
    {
        System.setProperties( sysProps );
    }

    
    @Test
    public void testShouldFailToSetLocalRepoLocationGloballyWhenItIsAFile()
    {

        tcb.setLocalRepositoryDirectory( lrd );

        try
        {
            tcb.setEnvironmentPaths( newRequest(), cli );
            fail( "Should not set local repo location to point to a file." );
        }
        catch ( IllegalArgumentException expected )
        {
        }
    }

    @Test
    public void testShouldFailToSetLocalRepoLocationFromRequestWhenItIsAFile()
    {
        try
        {
            tcb.setEnvironmentPaths( newRequest().setLocalRepositoryDirectory( lrd ), cli );
            fail( "Should not set local repo location to point to a file." );
        }
        catch ( IllegalArgumentException expected )
        {
        }
    }

    @Test
    public void testShouldSetLocalRepoLocationGlobally() throws IOException
    {
        File lrd = temporaryFolder.newFolder( "workdir" ).getCanonicalFile();
        tcb.setLocalRepositoryDirectory( lrd );
        tcb.setEnvironmentPaths( newRequest(), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testShouldSetLocalRepoLocationFromRequest()
        throws Exception
    {
        File lrd = temporaryFolder.newFolder( "workdir" ).getCanonicalFile();
        tcb.setEnvironmentPaths( newRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testRequestProvidedLocalRepoLocationShouldOverrideGlobal()
        throws Exception
    {
        File lrd = temporaryFolder.newFolder( "workdir" ).getCanonicalFile();
        File glrd = temporaryFolder.newFolder( "global-workdir" ).getCanonicalFile();

        tcb.setLocalRepositoryDirectory( glrd );
        tcb.setEnvironmentPaths( newRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testShouldSetWorkingDirectoryGlobally()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );
        tcb.setWorkingDirectory( wd );
        tcb.setEnvironmentPaths( newRequest(), cli );

        assertEquals( cli.getWorkingDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testShouldSetWorkingDirectoryFromRequest()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );

        InvocationRequest req = newRequest();
        req.setBaseDirectory( wd );


        tcb.setEnvironmentPaths( req, cli );

        assertEquals( cli.getWorkingDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testRequestProvidedWorkingDirectoryShouldOverrideGlobal()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );
        File gwd = temporaryFolder.newFolder( "global-workdir" );

        tcb.setWorkingDirectory( gwd );

        InvocationRequest req = newRequest();
        req.setBaseDirectory( wd );


        tcb.setEnvironmentPaths( req, cli );

        assertEquals( cli.getWorkingDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testShouldUseSystemOutLoggerWhenNoneSpecified()
        throws Exception
    {
        setupTempMavenHomeIfMissing( false );

        TestCommandLineBuilder tclb = new TestCommandLineBuilder();
        tclb.checkRequiredState();
    }

    private File setupTempMavenHomeIfMissing( boolean forceDummy )
        throws Exception
    {
        String mavenHome = System.getProperty( "maven.home" );

        File appDir;

        if ( forceDummy || ( mavenHome == null ) || !new File( mavenHome ).exists() )
        {
            appDir = temporaryFolder.newFolder( "invoker-tests", "maven-home" );

            File binDir = new File( appDir, "bin" );
            binDir.mkdirs();

            if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
            {
                createDummyFile( binDir, "mvn.bat" );
            }
            else
            {
                createDummyFile( binDir, "mvn" );
            }

            Properties props = System.getProperties();
            props.setProperty( "maven.home", appDir.getCanonicalPath() );

            System.setProperties( props );
        }
        else
        {
            appDir = new File( mavenHome );
        }

        return appDir;
    }

    @Test
    public void testShouldFailIfLoggerSetToNull() throws Exception
    {
        TestCommandLineBuilder tclb = new TestCommandLineBuilder();
        tclb.setLogger( null );

        try
        {
            tclb.checkRequiredState();
            fail( "Should not allow execution to proceed when logger is missing." );
        }
        catch ( IllegalStateException expected )
        {
        }
    }

    @Test
    public void testShouldFindDummyMavenExecutable()
        throws Exception
    {
        File dummyMavenHomeBin = temporaryFolder.newFolder( "invoker-tests", "dummy-maven-home", "bin" );

        File check;
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            check = createDummyFile( dummyMavenHomeBin, "mvn.bat" );
        }
        else
        {
            check = createDummyFile( dummyMavenHomeBin, "mvn" );
        }

        tcb.setMavenHome( dummyMavenHomeBin.getParentFile() );

        File mavenExe = tcb.findMavenExecutable();

        assertEquals( check.getCanonicalPath(), mavenExe.getCanonicalPath() );
    }

    @Test
    public void testShouldSetBatchModeFlagFromRequest()
    {

        tcb.setFlags( newRequest().setBatchMode( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-B" ) );
    }

    @Test
    public void testShouldSetOfflineFlagFromRequest()
    {

        tcb.setFlags( newRequest().setOffline( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-o" ) );
    }

    @Test
    public void testShouldSetUpdateSnapshotsFlagFromRequest()
    {

        tcb.setFlags( newRequest().setUpdateSnapshots( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-U" ) );
    }

    @Test
    public void testShouldSetDebugFlagFromRequest()
    {

        tcb.setFlags( newRequest().setDebug( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-X" ) );
    }

    @Test
    public void testShouldSetErrorFlagFromRequest()
    {

        tcb.setFlags( newRequest().setShowErrors( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-e" ) );
    }

    @Test
    public void testDebugOptionShouldMaskShowErrorsOption()
    {

        tcb.setFlags( newRequest().setDebug( true ).setShowErrors( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-X" ) );
        assertArgumentsNotPresent( cli, Collections.singleton( "-e" ) );
    }

    @Test
    public void testAlsoMake()
    {

        tcb.setReactorBehavior( newRequest().setAlsoMake( true ), cli );

        // -am is only useful with -pl
        assertArgumentsNotPresent( cli, Collections.singleton( "-am" ) );
    }

    @Test
    public void testProjectsAndAlsoMake()
    {

        tcb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMake( true ),
                                cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-am" );
    }

    @Test
    public void testAlsoMakeDependents()
    {

        tcb.setReactorBehavior( newRequest().setAlsoMakeDependents( true ), cli );

        // -amd is only useful with -pl
        assertArgumentsNotPresent( cli, Collections.singleton( "-amd" ) );
    }

    @Test
    public void testProjectsAndAlsoMakeDependents()
    {

        tcb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMakeDependents( true ),
                                cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-amd" );
    }

    @Test
    public void testProjectsAndAlsoMakeAndAlsoMakeDependents()
    {

        tcb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMake( true ).setAlsoMakeDependents( true ),
                                cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-am", "-amd" );
    }

    @Test
    public void testShouldSetResumeFrom()
    {

        tcb.setReactorBehavior( newRequest().setResumeFrom( ":module3" ), cli );

        assertArgumentsPresentInOrder( cli, "-rf", ":module3" );
    }

    @Test
    public void testShouldSetStrictChecksumPolityFlagFromRequest()
    {

        tcb.setFlags( newRequest().setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Fail ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-C" ) );
    }

    @Test
    public void testShouldSetLaxChecksumPolicyFlagFromRequest()
    {

        tcb.setFlags( newRequest().setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Warn ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-c" ) );
    }

    @Test
    public void testShouldSetFailAtEndFlagFromRequest()
    {

        tcb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailAtEnd ),
                                cli );

        assertArgumentsPresent( cli, Collections.singleton( "-fae" ) );
    }

    @Test
    public void testShouldSetFailNeverFlagFromRequest()
    {

        tcb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailNever ),
                                cli );

        assertArgumentsPresent( cli, Collections.singleton( "-fn" ) );
    }

    @Test
    public void testShouldUseDefaultOfFailFastWhenSpecifiedInRequest()
    {

        tcb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailFast ),
                                cli );

        Set<String> banned = new HashSet<>();
        banned.add( "-fae" );
        banned.add( "-fn" );

        assertArgumentsNotPresent( cli, banned );
    }

    @Test
    public void testShouldSpecifyFileOptionUsingNonStandardPomFileLocation()
        throws Exception
    {
        File projectDir =  temporaryFolder.newFolder( "invoker-tests", "file-option-nonstd-pom-file-location" );

        File pomFile = createDummyFile( projectDir, "non-standard-pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setPomFile( pomFile );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyFileOptionUsingNonStandardPomInBasedir()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "file-option-nonstd-basedir" );

        File basedir = createDummyFile( projectDir, "non-standard-pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setBaseDirectory( basedir );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldNotSpecifyFileOptionUsingStandardPomFileLocation()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-pom-file-location" );

        File pomFile = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setPomFile( pomFile );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( cli, args );
    }

    @Test
    public void testShouldNotSpecifyFileOptionUsingStandardPomInBasedir()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-is-pom-file" );

        File basedir = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setBaseDirectory( basedir );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( cli, args );
    }

    @Test
    public void testShouldUseDefaultPomFileWhenBasedirSpecifiedWithoutPomFileName()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-no-pom-filename" );


        InvocationRequest req = newRequest().setBaseDirectory( projectDir );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyPomFileWhenBasedirSpecifiedWithPomFileName()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-with-pom-filename" );


        InvocationRequest req = newRequest().setBaseDirectory( projectDir ).setPomFileName( "non-standard-pom.xml" );

        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir.getCanonicalFile(), cli.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyCustomUserSettingsLocationFromRequest()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "custom-settings" );

        File settingsFile = createDummyFile( projectDir, "settings.xml" );


        tcb.setSettingsLocation( newRequest().setUserSettingsFile( settingsFile ), cli );

        Set<String> args = new HashSet<>();
        args.add( "-s" );
        args.add( settingsFile.getCanonicalPath() );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyCustomGlobalSettingsLocationFromRequest()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "custom-settings" ).getCanonicalFile();

        File settingsFile = createDummyFile( projectDir, "settings.xml" );


        tcb.setSettingsLocation( newRequest().setGlobalSettingsFile( settingsFile ), cli );

        Set<String> args = new HashSet<>();
        args.add( "-gs" );
        args.add( settingsFile.getCanonicalPath() );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyCustomToolchainsLocationFromRequest()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "custom-toolchains" );

        File toolchainsFile = createDummyFile( projectDir, "toolchains.xml" );


        tcb.setToolchainsLocation( newRequest().setToolchainsFile( toolchainsFile ), cli );

        Set<String> args = new HashSet<>();
        args.add( "-t" );
        args.add( toolchainsFile.getCanonicalPath() );

        assertArgumentsPresent( cli, args );
    }

    @Test
    public void testShouldSpecifyCustomPropertyFromRequest()
    {

        Properties properties = new Properties();
        properties.setProperty( "key", "value" );

        tcb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key=value" );
    }

    @Test
    public void testShouldSpecifyCustomPropertyWithSpacesInValueFromRequest()
    {

        Properties properties = new Properties();
        properties.setProperty( "key", "value with spaces" );

        tcb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key=value with spaces" );
    }

    @Test
    public void testShouldSpecifyCustomPropertyWithSpacesInKeyFromRequest()
    {

        Properties properties = new Properties();
        properties.setProperty( "key with spaces", "value with spaces" );

        tcb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key with spaces=value with spaces" );
    }

    @Test
    public void testShouldSpecifySingleGoalFromRequest() throws CommandLineConfigurationException
    {

        List<String> goals = new ArrayList<>();
        goals.add( "test" );

        tcb.setGoals( newRequest().setGoals( goals ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "test" ) );
    }

    @Test
    public void testShouldSpecifyTwoGoalsFromRequest() throws CommandLineConfigurationException
    {
        List<String> goals = new ArrayList<>();
        goals.add( "test" );
        goals.add( "clean" );

        tcb.setGoals( newRequest().setGoals( goals ), cli );

        assertArgumentsPresent( cli, new HashSet<>( goals ) );
        assertArgumentsPresentInOrder( cli, goals );
    }

    @Test
    public void testShouldSpecifyThreadsFromRequest()
    {
        tcb.setThreads( newRequest().setThreads( "2.0C" ), cli );

        assertArgumentsPresentInOrder( cli, "-T", "2.0C" );
    }

    @Test
    public void testBuildTypicalMavenInvocationEndToEnd()
        throws Exception
    {
        File mavenDir = setupTempMavenHomeIfMissing( false );

        InvocationRequest request = newRequest();

        File projectDir = temporaryFolder.newFolder( "invoker-tests", "typical-end-to-end-cli-build" );

        request.setBaseDirectory( projectDir );

        Set<String> expectedArgs = new HashSet<>();
        Set<String> bannedArgs = new HashSet<>();

        createDummyFile( projectDir, "pom.xml" );

        bannedArgs.add( "-f" );
        bannedArgs.add( "pom.xml" );

        Properties properties = new Properties();
        // this is REALLY bad practice, but since it's just a test...
        properties.setProperty( "maven.tests.skip", "true" );

        expectedArgs.add( "maven.tests.skip=true" );

        request.setProperties( properties );

        request.setOffline( true );

        expectedArgs.add( "-o" );

        List<String> goals = new ArrayList<>();

        goals.add( "post-clean" );
        goals.add( "deploy" );
        goals.add( "site-deploy" );

        request.setGoals( goals );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();

        Commandline commandline = commandLineBuilder.build( request );

        assertArgumentsPresent( commandline, expectedArgs );
        assertArgumentsNotPresent( commandline, bannedArgs );
        assertArgumentsPresentInOrder( commandline, goals );

        String executable = commandline.getExecutable();

        assertTrue( executable.contains( new File( mavenDir, "bin/mvn" ).getCanonicalPath() ) );
        assertEquals( projectDir.getCanonicalPath(), commandline.getWorkingDirectory().getCanonicalPath() );
    }

    @Test
    public void testShouldSetEnvVar_MAVEN_TERMINATE_CMD()
        throws Exception
    {
        setupTempMavenHomeIfMissing( false );

        InvocationRequest request = newRequest();

        File projectDir = temporaryFolder.newFolder( "invoker-tests", "maven-terminate-cmd-options-set" );

        request.setBaseDirectory( projectDir );

        createDummyFile( projectDir, "pom.xml" );

        List<String> goals = new ArrayList<>();

        goals.add( "clean" );
        request.setGoals( goals );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();

        Commandline commandline = commandLineBuilder.build( request );

        String[] environmentVariables = commandline.getEnvironmentVariables();
        String envVarMavenTerminateCmd = null;
        for ( String envVar : environmentVariables )
        {
            if ( envVar.startsWith( "MAVEN_TERMINATE_CMD=" ) )
            {
                envVarMavenTerminateCmd = envVar;
                break;
            }
        }
        assertEquals( "MAVEN_TERMINATE_CMD=on", envVarMavenTerminateCmd );

    }

    @Test
    public void testShouldInsertActivatedProfiles()
        throws Exception
    {
        setupTempMavenHomeIfMissing( false );

        String profile1 = "profile-1";
        String profile2 = "profile-2";

        InvocationRequest request = newRequest();

        List<String> profiles = new ArrayList<>();
        profiles.add( profile1 );
        profiles.add( profile2 );

        request.setProfiles( profiles );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();

        Commandline commandline = commandLineBuilder.build( request );

        assertArgumentsPresentInOrder( commandline, "-P", profile1 + "," + profile2 );
    }

    @Test
    public void testShouldSetEnvVar_M2_HOME()
        throws Exception
    {
        Assume.assumeNotNull( System.getenv( "M2_HOME" ) );

        setupTempMavenHomeIfMissing( true );

        InvocationRequest request = newRequest();

        File projectDir = temporaryFolder.newFolder( "invoker-tests/maven-terminate-cmd-options-set" );

        request.setBaseDirectory( projectDir );

        createDummyFile( projectDir, "pom.xml" );

        List<String> goals = new ArrayList<>();

        goals.add( "clean" );
        request.setGoals( goals );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();
        File mavenHome2 = new File( System.getProperty( "maven.home" ) );
        commandLineBuilder.setMavenHome( mavenHome2 );

        Commandline commandline = commandLineBuilder.build( request );

        String[] environmentVariables = commandline.getEnvironmentVariables();
        String m2Home = null;
        for ( String envVar : environmentVariables )
        {
            if ( envVar.startsWith( "M2_HOME=" ) )
            {
                m2Home = envVar;
            }
        }
        assertEquals( "M2_HOME=" + mavenHome2.getAbsolutePath(), m2Home );
    }

    @Test
    public void testMvnCommand()
        throws Exception
    {
        assumeThat( "Test only works when called with surefire", System.getProperty( "maven.home" ),
                    is( notNullValue() ) );
        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();
        File mavenExecutable = new File( "mvnDebug" );
        commandLineBuilder.setMavenExecutable( mavenExecutable );
        File executable = commandLineBuilder.findMavenExecutable();
        assertTrue( "Expected executable to exist", executable.exists() );
        assertTrue( "Expected executable to be absolute", executable.isAbsolute() );
    }

    @Test
    public void testAddShellEnvironment()
        throws Exception
    {
        setupTempMavenHomeIfMissing( false );

        InvocationRequest request = newRequest();

        String envVar1Name = "VAR-1";
        String envVar1Value = "VAR-1-VALUE";

        String envVar2Name = "VAR-2";
        String envVar2Value = "VAR-2-VALUE";

        request.addShellEnvironment( envVar1Name, envVar1Value );
        request.addShellEnvironment( envVar2Name, envVar2Value );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();

        Commandline commandline = commandLineBuilder.build( request );

        assertEnvironmentVariablePresent( commandline, envVar1Name, envVar1Value );
        assertEnvironmentVariablePresent( commandline, envVar2Name, envVar2Value );
    }

    private void assertEnvironmentVariablePresent( Commandline cli, String varName, String varValue )
    {
        List<String> environmentVariables = Arrays.asList( cli.getEnvironmentVariables() );

        String expectedDeclaration = varName + "=" + varValue;

        assertTrue( "Environment variable setting: '" + expectedDeclaration + "' is mssing in "
            + environmentVariables, environmentVariables.contains( expectedDeclaration ) );
    }

    private void assertArgumentsPresentInOrder( Commandline cli, String... expected )
    {
        assertArgumentsPresentInOrder( cli, Arrays.asList( expected ) );
    }

    private void assertArgumentsPresentInOrder( Commandline cli, List<String> expected )
    {
        String[] arguments = cli.getArguments();

        int expectedCounter = 0;

        for ( String argument : arguments )
        {
            if ( argument.equals( expected.get( expectedCounter ) ) )
            {
                expectedCounter++;
            }
        }

        assertEquals( "Arguments: " + expected + " were not found or are in the wrong order: "
            + Arrays.asList( arguments ), expected.size(), expectedCounter );
    }

    private void assertArgumentsPresent( Commandline cli, Set<String> requiredArgs )
    {
        String[] argv = cli.getArguments();
        List<String> args = Arrays.asList( argv );

        for ( String arg : requiredArgs )
        {
            assertTrue( "Command-line argument: '" + arg + "' is missing in " + args, args.contains( arg ) );
        }
    }

    private void assertArgumentsNotPresent( Commandline cli, Set<String> bannedArgs )
    {
        String[] argv = cli.getArguments();
        List<String> args = Arrays.asList( argv );

        for ( String arg : bannedArgs )
        {
            assertFalse( "Command-line argument: '" + arg + "' should not be present.", args.contains( arg ) );
        }
    }

    private File createDummyFile( File directory, String filename )
        throws IOException
    {
        File dummyFile = new File( directory, filename );
        
        try ( FileWriter writer = new FileWriter( dummyFile ) )
        {
            writer.write( "This is a dummy file." );
        }

        return dummyFile;
    }

    private static final class TestCommandLineBuilder
        extends MavenCommandLineBuilder
    {
        public void checkRequiredState()
            throws IOException
        {
            super.checkRequiredState();
        }

        public File findMavenExecutable()
            throws CommandLineConfigurationException, IOException
        {
            return super.findMavenExecutable();
        }

        public void setEnvironmentPaths( InvocationRequest request, Commandline cli )
        {
            super.setEnvironmentPaths( request, cli );
        }

        public void setFlags( InvocationRequest request, Commandline cli )
        {
            super.setFlags( request, cli );
        }

        public void setGoals( InvocationRequest request, Commandline cli ) throws CommandLineConfigurationException
        {
            super.setGoals( request, cli );
        }

        public void setPomLocation( InvocationRequest request, Commandline cli )
        {
            super.setPomLocation( request, cli );
        }

        public void setProperties( InvocationRequest request, Commandline cli )
        {
            super.setProperties( request, cli );
        }

        public void setReactorBehavior( InvocationRequest request, Commandline cli )
        {
            super.setReactorBehavior( request, cli );
        }

        public void setSettingsLocation( InvocationRequest request, Commandline cli )
        {
            super.setSettingsLocation( request, cli );
        }

        public void setShellEnvironment( InvocationRequest request, Commandline cli )
            throws CommandLineConfigurationException
        {
            super.setShellEnvironment( request, cli );
        }

    }

    private InvocationRequest newRequest()
    {
        return new DefaultInvocationRequest();
    }

}
