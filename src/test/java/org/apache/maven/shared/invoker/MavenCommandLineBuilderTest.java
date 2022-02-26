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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
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
    private MavenCommandLineBuilder mclb = new MavenCommandLineBuilder();
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

        mclb.setLocalRepositoryDirectory( lrd );

        try
        {
            mclb.setLocalRepository( newRequest(), cli );
            fail( "Should not set local repo location to point to a file." );
        }
        catch ( IllegalArgumentException expected )
        {
        }
    }

    @Test
    public void testShouldFailToSetLocalRepoLocationFromRequestWhenItIsAFile()
    {
        InvocationRequest request = newRequest().setLocalRepositoryDirectory( lrd );
        try
        {
            mclb.setLocalRepository( request, cli );
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
        mclb.setLocalRepositoryDirectory( lrd );
        mclb.setLocalRepository( newRequest(), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testShouldSetLocalRepoLocationFromRequest()
        throws Exception
    {
        File lrd = temporaryFolder.newFolder( "workdir" ).getCanonicalFile();
        mclb.setLocalRepository( newRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testRequestProvidedLocalRepoLocationShouldOverrideGlobal()
        throws Exception
    {
        File lrd = temporaryFolder.newFolder( "workdir" ).getCanonicalFile();
        File glrd = temporaryFolder.newFolder( "global-workdir" ).getCanonicalFile();

        mclb.setLocalRepositoryDirectory( glrd );
        mclb.setLocalRepository( newRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "maven.repo.local=" + lrd.getPath() );
    }

    @Test
    public void testShouldSetWorkingDirectoryGlobally()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );

        mclb.setBaseDirectory( wd );
        Commandline commandline = mclb.build( newRequest() );

        assertEquals( commandline.getWorkingDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testShouldSetWorkingDirectoryFromRequest()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );

        InvocationRequest req = newRequest();
        req.setBaseDirectory( wd );

        mclb.setupBaseDirectory( req );

        assertEquals( mclb.getBaseDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testRequestProvidedWorkingDirectoryShouldOverrideGlobal()
        throws Exception
    {
        File wd = temporaryFolder.newFolder( "workdir" );
        File gwd = temporaryFolder.newFolder( "global-workdir" );

        mclb.setBaseDirectory( gwd );

        InvocationRequest req = newRequest();
        req.setBaseDirectory( wd );

        mclb.setupBaseDirectory( req );

        assertEquals( mclb.getBaseDirectory(), wd.getCanonicalFile() );
    }

    @Test
    public void testShouldUseSystemOutLoggerWhenNoneSpecified()
        throws Exception
    {
        setupTempMavenHomeIfMissing( false );

        mclb.checkRequiredState();
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
    public void testShouldFailIfLoggerSetToNull()
    {
        mclb.setLogger( null );

        try
        {
            mclb.checkRequiredState();
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

        mclb.setMavenHome( dummyMavenHomeBin.getParentFile() );
        mclb.setupMavenExecutable( newRequest() );

        assertEquals( check.getCanonicalPath(), mclb.getMavenExecutable().getCanonicalPath() );
    }

    @Test
    public void testShouldFindDummyMavenExecutableWithMavenHomeFromRequest()
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

        // default value should be not used
        mclb.setMavenHome( new File( "not-present-1234" ) );
        mclb.build( newRequest().setMavenHome( dummyMavenHomeBin.getParentFile() ) );

        assertEquals( check.getCanonicalPath(), mclb.getMavenExecutable().getCanonicalPath() );
    }

    @Test
    public void testShouldSetBatchModeFlagFromRequest()
    {

        mclb.setFlags( newRequest().setBatchMode( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-B" ) );
    }

    @Test
    public void testShouldSetOfflineFlagFromRequest()
    {

        mclb.setFlags( newRequest().setOffline( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-o" ) );
    }

    @Test
    public void testShouldSetUpdateSnapshotsFlagFromRequest()
    {

        mclb.setFlags( newRequest().setUpdateSnapshots( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-U" ) );
    }

    @Test
    public void testShouldSetDebugFlagFromRequest()
    {

        mclb.setFlags( newRequest().setDebug( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-X" ) );
    }

    @Test
    public void testShouldSetErrorFlagFromRequest()
    {

        mclb.setFlags( newRequest().setShowErrors( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-e" ) );
    }

    @Test
    public void testShouldSetQuietFlagFromRequest()
    {

        mclb.setFlags( newRequest().setQuiet( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-q" ) );
    }

    @Test
    public void testShouldSetNonRecursiveFlagsFromRequest()
    {
        mclb.setFlags( newRequest().setRecursive( false ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-N" ) );
    }

    @Test
    public void testShouldSetShowVersionFlagsFromRequest()
    {
        mclb.setFlags( newRequest().setShowVersion( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-V" ) );
    }

    @Test
    public void testDebugOptionShouldMaskShowErrorsOption()
    {

        mclb.setFlags( newRequest().setDebug( true ).setShowErrors( true ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-X" ) );
        assertArgumentsNotPresent( cli, Collections.singleton( "-e" ) );
    }

    @Test
    public void testShouldSetBuilderIdOptionsFromRequest()
    {
        mclb.setFlags( newRequest().setBuilder( "builder-id-123" ), cli );

        assertArgumentsPresentInOrder( cli, "-b", "builder-id-123" );
    }

    @Test
    public void testAlsoMake()
    {

        mclb.setReactorBehavior( newRequest().setAlsoMake( true ), cli );

        // -am is only useful with -pl
        assertArgumentsNotPresent( cli, Collections.singleton( "-am" ) );
    }

    @Test
    public void testProjectsAndAlsoMake()
    {

        mclb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMake( true ),
                                 cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-am" );
    }

    @Test
    public void testAlsoMakeDependents()
    {

        mclb.setReactorBehavior( newRequest().setAlsoMakeDependents( true ), cli );

        // -amd is only useful with -pl
        assertArgumentsNotPresent( cli, Collections.singleton( "-amd" ) );
    }

    @Test
    public void testProjectsAndAlsoMakeDependents()
    {

        mclb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMakeDependents( true ),
                                cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-amd" );
    }

    @Test
    public void testProjectsAndAlsoMakeAndAlsoMakeDependents()
    {

        mclb.setReactorBehavior( newRequest().setProjects( Collections.singletonList( "proj1" ) ).setAlsoMake( true ).setAlsoMakeDependents( true ),
                                cli );

        assertArgumentsPresentInOrder( cli, "-pl", "proj1", "-am", "-amd" );
    }

    @Test
    public void testShouldSetResumeFrom()
    {

        mclb.setReactorBehavior( newRequest().setResumeFrom( ":module3" ), cli );

        assertArgumentsPresentInOrder( cli, "-rf", ":module3" );
    }

    @Test
    public void testShouldSetStrictChecksumPolityFlagFromRequest()
    {

        mclb.setFlags( newRequest().setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Fail ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-C" ) );
    }

    @Test
    public void testShouldSetLaxChecksumPolicyFlagFromRequest()
    {

        mclb.setFlags( newRequest().setGlobalChecksumPolicy( InvocationRequest.CheckSumPolicy.Warn ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "-c" ) );
    }

    @Test
    public void testShouldSetFailAtEndFlagFromRequest()
    {

        mclb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailAtEnd ),
                                cli );

        assertArgumentsPresent( cli, Collections.singleton( "-fae" ) );
    }

    @Test
    public void testShouldSetFailNeverFlagFromRequest()
    {

        mclb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailNever ),
                                cli );

        assertArgumentsPresent( cli, Collections.singleton( "-fn" ) );
    }


    @Test
    public void testShouldAddArg() throws CommandLineConfigurationException
    {
        InvocationRequest request = newRequest()
            .addArg( "arg1" )
            .addArg( "arg2" )
            .setQuiet( true )
            .setBuilder( "bId" );

        Commandline commandline = mclb.build( request );

        String[] arguments = commandline.getArguments();

        assertArrayEquals( Arrays.asList( "-b", "bId", "-q", "arg1",  "arg2" ).toArray(), arguments );
    }

    @Test
    public void testShouldUseDefaultOfFailFastWhenSpecifiedInRequest()
    {

        mclb.setReactorBehavior( newRequest().setReactorFailureBehavior( InvocationRequest.ReactorFailureBehavior.FailFast ),
                                cli );

        Set<String> banned = new HashSet<>();
        banned.add( "-fae" );
        banned.add( "-fn" );

        assertArgumentsNotPresent( cli, banned );
    }

    @Test
    public void testShouldSetNoTransferProgressFlagFromRequest()
    {
        mclb.setFlags( newRequest().setNoTransferProgress( true ), cli );
        assertArgumentsPresent( cli, Collections.singleton( "-ntp" ) );
    }

    @Test
    public void testShouldSpecifyFileOptionUsingNonStandardPomFileLocation()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "file-option-nonstd-pom-file-location" );

        File pomFile = createDummyFile( projectDir, "non-standard-pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setPomFile( pomFile );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( commandline, args );
    }

    @Test
    public void testShouldNotSpecifyFileOptionUsingStandardPomFileLocation()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-pom-file-location" );

        File pomFile = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setPomFile( pomFile );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( commandline, args );
    }

    @Test
    public void testShouldSetPomForOutsideWorkspace()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-pom-file-location" );

        File outsidePom = temporaryFolder.newFile( "pom.xml" );


        InvocationRequest req = newRequest()
            .setBaseDirectory( projectDir )
            .setPomFile( outsidePom );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( outsidePom.getCanonicalPath() );

        assertArgumentsPresent( commandline, args );
    }

    @Test
    public void testShouldNotSpecifyFileOptionUsingStandardPomInBasedir()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-is-pom-file" );

        File basedir = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();


        InvocationRequest req = newRequest().setBaseDirectory( basedir );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( commandline, args );
    }

    @Test
    public void testShouldUseDefaultPomFileWhenBasedirSpecifiedWithoutPomFileName()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-no-pom-filename" );


        InvocationRequest req = newRequest().setBaseDirectory( projectDir );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( commandline, args );
    }

    @Test
    public void testShouldSpecifyPomFileWhenBasedirSpecifiedWithPomFileName()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "std-basedir-with-pom-filename" );

        InvocationRequest req = newRequest().setBaseDirectory( projectDir ).setPomFileName( "non-standard-pom.xml" );

        Commandline commandline = mclb.build( req );

        assertEquals( projectDir.getCanonicalFile(), commandline.getWorkingDirectory() );

        Set<String> args = new HashSet<>();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( commandline, args );
    }

    @Test
    public void testShouldSpecifyCustomUserSettingsLocationFromRequest()
        throws Exception
    {
        File projectDir = temporaryFolder.newFolder( "invoker-tests", "custom-settings" );

        File settingsFile = createDummyFile( projectDir, "settings.xml" );


        mclb.setSettingsLocation( newRequest().setUserSettingsFile( settingsFile ), cli );

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


        mclb.setSettingsLocation( newRequest().setGlobalSettingsFile( settingsFile ), cli );

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


        mclb.setToolchainsLocation( newRequest().setToolchainsFile( toolchainsFile ), cli );

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

        mclb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key=value" );
    }

    @Test
    public void testShouldSpecifyCustomPropertyWithSpacesInValueFromRequest()
    {

        Properties properties = new Properties();
        properties.setProperty( "key", "value with spaces" );

        mclb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key=value with spaces" );
    }

    @Test
    public void testShouldSpecifyCustomPropertyWithSpacesInKeyFromRequest()
    {

        Properties properties = new Properties();
        properties.setProperty( "key with spaces", "value with spaces" );

        mclb.setProperties( newRequest().setProperties( properties ), cli );

        assertArgumentsPresentInOrder( cli, "-D", "key with spaces=value with spaces" );
    }

    @Test
    public void testShouldSpecifySingleGoalFromRequest() throws CommandLineConfigurationException
    {

        List<String> goals = new ArrayList<>();
        goals.add( "test" );

        mclb.setGoals( newRequest().setGoals( goals ), cli );

        assertArgumentsPresent( cli, Collections.singleton( "test" ) );
    }

    @Test
    public void testShouldSpecifyTwoGoalsFromRequest() throws CommandLineConfigurationException
    {
        List<String> goals = new ArrayList<>();
        goals.add( "test" );
        goals.add( "clean" );

        mclb.setGoals( newRequest().setGoals( goals ), cli );

        assertArgumentsPresent( cli, new HashSet<>( goals ) );
        assertArgumentsPresentInOrder( cli, goals );
    }

    @Test
    public void testShouldSpecifyThreadsFromRequest()
    {
        mclb.setThreads( newRequest().setThreads( "2.0C" ), cli );

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

        Commandline commandline = mclb.build( request );

        assertArgumentsPresent( commandline, expectedArgs );
        assertArgumentsNotPresent( commandline, bannedArgs );
        assertArgumentsPresentInOrder( commandline, goals );

        String executable = commandline.getExecutable();

        assertTrue( executable.contains( new File( mavenDir, "bin/mvn" ).getCanonicalPath() ) );
        assertEquals( projectDir.getCanonicalPath(), commandline.getWorkingDirectory().getCanonicalPath() );
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

        Commandline commandline = mclb.build( request );

        assertArgumentsPresentInOrder( commandline, "-P", profile1 + "," + profile2 );
    }

    @Test
    public void testMvnExecutableFromInvoker()
        throws Exception
    {
        assumeThat( "Test only works when maven.home is set",
            System.getProperty( "maven.home" ), is(notNullValue()));

        File mavenExecutable = new File( "mvnDebug" );

        mclb.setMavenExecutable( mavenExecutable );
        mclb.build( newRequest() );

        assertTrue( "Expected executable to exist", mclb.getMavenExecutable().exists() );
        assertTrue( "Expected executable to be absolute", mclb.getMavenExecutable().isAbsolute() );
        assertTrue( "Expected mvnDebug as command mvnDebug", mclb.getMavenExecutable().getName().contains( "mvnDebug" ) );
    }

    @Test
    public void testMvnExecutableFormRequest()
        throws Exception
    {
        assumeThat( "Test only works when maven.home is set",
            System.getProperty( "maven.home" ), is(notNullValue()));

        File mavenExecutable = new File( "mvnDebug" );

        mclb.build( newRequest().setMavenExecutable( mavenExecutable ) );

        assertTrue( "Expected executable to exist", mclb.getMavenExecutable().exists() );
        assertTrue( "Expected executable to be absolute", mclb.getMavenExecutable().isAbsolute() );
        assertTrue( "Expected mvnDebug as command", mclb.getMavenExecutable().getName().contains( "mvnDebug" ) );
    }

    @Test
    public void testDefaultMavenCommand()
        throws Exception
    {
        assumeThat( "Test only works when maven.home is set",
            System.getProperty( "maven.home" ), is(notNullValue()));

        mclb.build( newRequest() );

        assertTrue( "Expected executable to exist", mclb.getMavenExecutable().exists() );
        assertTrue( "Expected executable to be absolute", mclb.getMavenExecutable().isAbsolute() );
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

        Commandline commandline = mclb.build( request );

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

    private InvocationRequest newRequest()
    {
        return new DefaultInvocationRequest();
    }

}
