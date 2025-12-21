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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.utils.Os;
import org.apache.maven.shared.utils.cli.Commandline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MavenCommandLineBuilderTest {
    @TempDir
    private Path temporaryFolder;

    private Properties sysProps;
    private File lrd;
    private MavenCommandLineBuilder mclb = new MavenCommandLineBuilder();
    private Commandline cli = new Commandline();

    @BeforeEach
    void setUp() throws IOException {
        sysProps = System.getProperties();
        Properties p = new Properties(sysProps);

        System.setProperties(p);

        lrd = Files.createTempFile(temporaryFolder, "", "").toFile();
    }

    @AfterEach
    void tearDown() {
        System.setProperties(sysProps);
    }

    @Test
    void shouldFailToSetLocalRepoLocationGloballyWhenItIsAFile() {

        mclb.setLocalRepositoryDirectory(lrd);

        InvocationRequest request = newRequest();
        assertThrows(IllegalArgumentException.class, () -> mclb.setLocalRepository(request, cli));
    }

    @Test
    void shouldFailToSetLocalRepoLocationFromRequestWhenItIsAFile() {
        InvocationRequest request = newRequest().setLocalRepositoryDirectory(lrd);

        assertThrows(IllegalArgumentException.class, () -> mclb.setLocalRepository(request, cli));
    }

    @Test
    void shouldSetLocalRepoLocationGlobally() throws Exception {
        File lrd = Files.createDirectory(temporaryFolder.resolve("workdir"))
                .toFile()
                .getCanonicalFile();
        mclb.setLocalRepositoryDirectory(lrd);
        mclb.setLocalRepository(newRequest(), cli);

        assertArgumentsPresentInOrder(cli, "-D", "maven.repo.local=" + lrd.getPath());
    }

    @Test
    void shouldSetLocalRepoLocationFromRequest() throws Exception {
        File lrd = Files.createDirectory(temporaryFolder.resolve("workdir"))
                .toFile()
                .getCanonicalFile();
        mclb.setLocalRepository(newRequest().setLocalRepositoryDirectory(lrd), cli);

        assertArgumentsPresentInOrder(cli, "-D", "maven.repo.local=" + lrd.getPath());
    }

    @Test
    void requestProvidedLocalRepoLocationShouldOverrideGlobal() throws Exception {
        File lrd = Files.createDirectory(temporaryFolder.resolve("workdir"))
                .toFile()
                .getCanonicalFile();
        File glrd = Files.createDirectory(temporaryFolder.resolve("global-workdir"))
                .toFile()
                .getCanonicalFile();

        mclb.setLocalRepositoryDirectory(glrd);
        mclb.setLocalRepository(newRequest().setLocalRepositoryDirectory(lrd), cli);

        assertArgumentsPresentInOrder(cli, "-D", "maven.repo.local=" + lrd.getPath());
    }

    @Test
    void shouldSetWorkingDirectoryGlobally() throws Exception {
        File wd = Files.createDirectory(temporaryFolder.resolve("workdir")).toFile();

        mclb.setBaseDirectory(wd);
        Commandline commandline = mclb.build(newRequest());

        assertEquals(commandline.getWorkingDirectory(), wd.getCanonicalFile());
    }

    @Test
    void shouldSetWorkingDirectoryFromRequest() throws Exception {
        File wd = Files.createDirectory(temporaryFolder.resolve("workdir")).toFile();

        InvocationRequest req = newRequest();
        req.setBaseDirectory(wd);

        mclb.setupBaseDirectory(req);

        assertEquals(mclb.getBaseDirectory(), wd.getCanonicalFile());
    }

    @Test
    void requestProvidedWorkingDirectoryShouldOverrideGlobal() throws Exception {
        File wd = Files.createDirectory(temporaryFolder.resolve("workdir")).toFile();
        File gwd =
                Files.createDirectory(temporaryFolder.resolve("global-workdir")).toFile();

        mclb.setBaseDirectory(gwd);

        InvocationRequest req = newRequest();
        req.setBaseDirectory(wd);

        mclb.setupBaseDirectory(req);

        assertEquals(mclb.getBaseDirectory(), wd.getCanonicalFile());
    }

    @Test
    void shouldUseSystemOutLoggerWhenNoneSpecified() throws Exception {
        setupTempMavenHomeIfMissing(false);

        mclb.checkRequiredState();
    }

    private File setupTempMavenHomeIfMissing(boolean forceDummy) throws Exception {
        String mavenHome = System.getProperty("maven.home");

        File appDir;

        if (forceDummy || (mavenHome == null) || !new File(mavenHome).exists()) {
            appDir = Files.createDirectories(
                            temporaryFolder.resolve("invoker-tests").resolve("maven-home"))
                    .toFile();

            File binDir = new File(appDir, "bin");
            binDir.mkdirs();

            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                createDummyFile(binDir, "mvn.bat");
            } else {
                createDummyFile(binDir, "mvn");
            }

            Properties props = System.getProperties();
            props.setProperty("maven.home", appDir.getCanonicalPath());

            System.setProperties(props);
        } else {
            appDir = new File(mavenHome);
        }

        return appDir;
    }

    @Test
    void shouldFailIfLoggerSetToNull() {
        mclb.setLogger(null);

        assertThrows(IllegalStateException.class, () -> mclb.checkRequiredState());
    }

    @Test
    void shouldFindDummyMavenExecutable() throws Exception {
        File dummyMavenHomeBin = Files.createDirectories(temporaryFolder
                        .resolve("invoker-tests")
                        .resolve("dummy-maven-home")
                        .resolve("bin"))
                .toFile();

        File check;
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            check = createDummyFile(dummyMavenHomeBin, "mvn.bat");
        } else {
            check = createDummyFile(dummyMavenHomeBin, "mvn");
        }

        mclb.setMavenHome(dummyMavenHomeBin.getParentFile());
        mclb.setupMavenExecutable(newRequest());

        assertEquals(check.getCanonicalPath(), mclb.getMavenExecutable().getCanonicalPath());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldFindDummyPS1MavenExecutable() throws Exception {
        File dummyMavenHomeBin = Files.createDirectories(temporaryFolder
                        .resolve("invoker-tests")
                        .resolve("dummy-maven-home")
                        .resolve("bin"))
                .toFile();

        File check = createDummyFile(dummyMavenHomeBin, "mvn.ps1");
        mclb.setMavenHome(dummyMavenHomeBin.getParentFile());
        mclb.setupMavenExecutable(newRequest());

        assertEquals(check.getCanonicalPath(), mclb.getMavenExecutable().getCanonicalPath());
    }

    @Test
    void shouldFindDummyMavenExecutableWithMavenHomeFromRequest() throws Exception {
        File dummyMavenHomeBin = Files.createDirectories(temporaryFolder
                        .resolve("invoker-tests")
                        .resolve("dummy-maven-home")
                        .resolve("bin"))
                .toFile();

        File check;
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            check = createDummyFile(dummyMavenHomeBin, "mvn.bat");
        } else {
            check = createDummyFile(dummyMavenHomeBin, "mvn");
        }

        // default value should be not used
        mclb.setMavenHome(new File("not-present-1234"));
        mclb.build(newRequest().setMavenHome(dummyMavenHomeBin.getParentFile()));

        assertEquals(check.getCanonicalPath(), mclb.getMavenExecutable().getCanonicalPath());
    }

    @Test
    void shouldSetBatchModeFlagFromRequest() {

        mclb.setFlags(newRequest().setBatchMode(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-B"));
    }

    @Test
    void shouldSetOfflineFlagFromRequest() {

        mclb.setFlags(newRequest().setOffline(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-o"));
    }

    @Test
    void shouldSetUpdateSnapshotsFlagFromRequest() {

        mclb.setFlags(newRequest().setUpdateSnapshots(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-U"));
    }

    // JUnit5: test methods don't need to be public
    @Test
    void shouldSetUpdateSnapshotsPolicyAlwaysFromRequest() {
        mclb.setFlags(newRequest().setUpdateSnapshotsPolicy(UpdateSnapshotsPolicy.ALWAYS), cli);

        assertArgumentsPresent(cli, Collections.singleton("-U"));
        assertArgumentsNotPresent(cli, Collections.singleton("-nsu"));
    }

    @Test
    void shouldSetUpdateSnapshotsPolicyDefaultFromRequest() {
        mclb.setFlags(newRequest().setUpdateSnapshotsPolicy(UpdateSnapshotsPolicy.DEFAULT), cli);

        Set<String> args = new HashSet<>();
        args.add("-U");
        args.add("-nsu");
        assertArgumentsNotPresent(cli, args);
    }

    @Test
    void shouldSetUpdateSnapshotsPolicyNeverFromRequest() {
        mclb.setFlags(newRequest().setUpdateSnapshotsPolicy(UpdateSnapshotsPolicy.NEVER), cli);

        assertArgumentsPresent(cli, Collections.singleton("-nsu"));
        assertArgumentsNotPresent(cli, Collections.singleton("-U"));
    }

    @Test
    void shouldSetDebugFlagFromRequest() {

        mclb.setFlags(newRequest().setDebug(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-X"));
    }

    @Test
    void shouldSetErrorFlagFromRequest() {

        mclb.setFlags(newRequest().setShowErrors(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-e"));
    }

    @Test
    void shouldSetQuietFlagFromRequest() {

        mclb.setFlags(newRequest().setQuiet(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-q"));
    }

    @Test
    void shouldSetNonRecursiveFlagsFromRequest() {
        mclb.setFlags(newRequest().setRecursive(false), cli);

        assertArgumentsPresent(cli, Collections.singleton("-N"));
    }

    @Test
    void shouldSetShowVersionFlagsFromRequest() {
        mclb.setFlags(newRequest().setShowVersion(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-V"));
    }

    @Test
    void debugOptionShouldMaskShowErrorsOption() {

        mclb.setFlags(newRequest().setDebug(true).setShowErrors(true), cli);

        assertArgumentsPresent(cli, Collections.singleton("-X"));
        assertArgumentsNotPresent(cli, Collections.singleton("-e"));
    }

    @Test
    void shouldSetBuilderIdOptionsFromRequest() {
        mclb.setFlags(newRequest().setBuilder("builder-id-123"), cli);

        assertArgumentsPresentInOrder(cli, "-b", "builder-id-123");
    }

    @Test
    void alsoMake() {

        mclb.setReactorBehavior(newRequest().setAlsoMake(true), cli);

        // -am is only useful with -pl
        assertArgumentsNotPresent(cli, Collections.singleton("-am"));
    }

    @Test
    void projectsAndAlsoMake() {

        mclb.setReactorBehavior(
                newRequest().setProjects(Collections.singletonList("proj1")).setAlsoMake(true), cli);

        assertArgumentsPresentInOrder(cli, "-pl", "proj1", "-am");
    }

    @Test
    void alsoMakeDependents() {

        mclb.setReactorBehavior(newRequest().setAlsoMakeDependents(true), cli);

        // -amd is only useful with -pl
        assertArgumentsNotPresent(cli, Collections.singleton("-amd"));
    }

    @Test
    void projectsAndAlsoMakeDependents() {

        mclb.setReactorBehavior(
                newRequest().setProjects(Collections.singletonList("proj1")).setAlsoMakeDependents(true), cli);

        assertArgumentsPresentInOrder(cli, "-pl", "proj1", "-amd");
    }

    @Test
    void projectsAndAlsoMakeAndAlsoMakeDependents() {

        mclb.setReactorBehavior(
                newRequest()
                        .setProjects(Collections.singletonList("proj1"))
                        .setAlsoMake(true)
                        .setAlsoMakeDependents(true),
                cli);

        assertArgumentsPresentInOrder(cli, "-pl", "proj1", "-am", "-amd");
    }

    @Test
    void shouldSetResumeFrom() {

        mclb.setReactorBehavior(newRequest().setResumeFrom(":module3"), cli);

        assertArgumentsPresentInOrder(cli, "-rf", ":module3");
    }

    @Test
    void shouldSetStrictChecksumPolityFlagFromRequest() {

        mclb.setFlags(newRequest().setGlobalChecksumPolicy(InvocationRequest.CheckSumPolicy.Fail), cli);

        assertArgumentsPresent(cli, Collections.singleton("-C"));
    }

    @Test
    void shouldSetLaxChecksumPolicyFlagFromRequest() {

        mclb.setFlags(newRequest().setGlobalChecksumPolicy(InvocationRequest.CheckSumPolicy.Warn), cli);

        assertArgumentsPresent(cli, Collections.singleton("-c"));
    }

    @Test
    void shouldSetFailAtEndFlagFromRequest() {

        mclb.setReactorBehavior(
                newRequest().setReactorFailureBehavior(InvocationRequest.ReactorFailureBehavior.FailAtEnd), cli);

        assertArgumentsPresent(cli, Collections.singleton("-fae"));
    }

    @Test
    void shouldSetFailNeverFlagFromRequest() {

        mclb.setReactorBehavior(
                newRequest().setReactorFailureBehavior(InvocationRequest.ReactorFailureBehavior.FailNever), cli);

        assertArgumentsPresent(cli, Collections.singleton("-fn"));
    }

    @Test
    void shouldAddArg() throws Exception {
        InvocationRequest request =
                newRequest().addArg("arg1").addArg("arg2").setQuiet(true).setBuilder("bId");

        Commandline commandline = mclb.build(request);

        String[] arguments = commandline.getArguments();

        assertArrayEquals(Arrays.asList("-b", "bId", "-q", "arg1", "arg2").toArray(), arguments);
    }

    @Test
    void shouldUseDefaultOfFailFastWhenSpecifiedInRequest() {

        mclb.setReactorBehavior(
                newRequest().setReactorFailureBehavior(InvocationRequest.ReactorFailureBehavior.FailFast), cli);

        Set<String> banned = new HashSet<>();
        banned.add("-fae");
        banned.add("-fn");

        assertArgumentsNotPresent(cli, banned);
    }

    @Test
    void shouldSetNoTransferProgressFlagFromRequest() {
        mclb.setFlags(newRequest().setNoTransferProgress(true), cli);
        assertArgumentsPresent(cli, Collections.singleton("-ntp"));
    }

    @Test
    void shouldSetIgnoreTransitiveRepositoriesFromRequest() {
        mclb.setFlags(newRequest().setIgnoreTransitiveRepositories(true), cli);
        assertArgumentsPresent(cli, Collections.singleton("-itr"));
    }

    @Test
    void shouldSpecifyFileOptionUsingNonStandardPomFileLocation() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("file-option-nonstd-pom-file-location"))
                .toFile();

        File pomFile = createDummyFile(projectDir, "non-standard-pom.xml").getCanonicalFile();

        InvocationRequest req = newRequest().setPomFile(pomFile);

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add("non-standard-pom.xml");

        assertArgumentsPresent(commandline, args);
    }

    @Test
    void shouldNotSpecifyFileOptionUsingStandardPomFileLocation() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("std-pom-file-location"))
                .toFile();

        File pomFile = createDummyFile(projectDir, "pom.xml").getCanonicalFile();

        InvocationRequest req = newRequest().setPomFile(pomFile);

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add("pom.xml");

        assertArgumentsNotPresent(commandline, args);
    }

    @Test
    void shouldSetPomForOutsideWorkspace() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("std-pom-file-location"))
                .toFile();

        File outsidePom = Files.createFile(temporaryFolder.resolve("pom.xml")).toFile();

        InvocationRequest req = newRequest().setBaseDirectory(projectDir).setPomFile(outsidePom);

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add(outsidePom.getCanonicalPath());

        assertArgumentsPresent(commandline, args);
    }

    @Test
    void shouldNotSpecifyFileOptionUsingStandardPomInBasedir() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("std-basedir-is-pom-file"))
                .toFile();

        File basedir = createDummyFile(projectDir, "pom.xml").getCanonicalFile();

        InvocationRequest req = newRequest().setBaseDirectory(basedir);

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add("pom.xml");

        assertArgumentsNotPresent(commandline, args);
    }

    @Test
    void shouldUseDefaultPomFileWhenBasedirSpecifiedWithoutPomFileName() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("std-basedir-no-pom-filename"))
                .toFile();

        InvocationRequest req = newRequest().setBaseDirectory(projectDir);

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add("pom.xml");

        assertArgumentsNotPresent(commandline, args);
    }

    @Test
    void shouldSpecifyPomFileWhenBasedirSpecifiedWithPomFileName() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("std-basedir-with-pom-filename"))
                .toFile();

        InvocationRequest req = newRequest().setBaseDirectory(projectDir).setPomFileName("non-standard-pom.xml");

        Commandline commandline = mclb.build(req);

        assertEquals(projectDir.getCanonicalFile(), commandline.getWorkingDirectory());

        Set<String> args = new HashSet<>();
        args.add("-f");
        args.add("non-standard-pom.xml");

        assertArgumentsPresent(commandline, args);
    }

    @Test
    void shouldSpecifyCustomUserSettingsLocationFromRequest() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("custom-settings"))
                .toFile();

        File settingsFile = createDummyFile(projectDir, "settings.xml");

        mclb.setSettingsLocation(newRequest().setUserSettingsFile(settingsFile), cli);

        Set<String> args = new HashSet<>();
        args.add("-s");
        args.add(settingsFile.getCanonicalPath());

        assertArgumentsPresent(cli, args);
    }

    @Test
    void shouldSpecifyCustomGlobalSettingsLocationFromRequest() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("custom-settings"))
                .toFile()
                .getCanonicalFile();

        File settingsFile = createDummyFile(projectDir, "settings.xml");

        mclb.setSettingsLocation(newRequest().setGlobalSettingsFile(settingsFile), cli);

        Set<String> args = new HashSet<>();
        args.add("-gs");
        args.add(settingsFile.getCanonicalPath());

        assertArgumentsPresent(cli, args);
    }

    @Test
    void shouldSpecifyCustomToolchainsLocationFromRequest() throws Exception {
        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("custom-toolchains"))
                .toFile();

        File toolchainsFile = createDummyFile(projectDir, "toolchains.xml");

        mclb.setToolchainsLocation(newRequest().setToolchainsFile(toolchainsFile), cli);

        Set<String> args = new HashSet<>();
        args.add("-t");
        args.add(toolchainsFile.getCanonicalPath());

        assertArgumentsPresent(cli, args);
    }

    @Test
    void shouldSpecifyCustomPropertyFromRequest() {

        Properties properties = new Properties();
        properties.setProperty("key", "value");

        mclb.setProperties(newRequest().setProperties(properties), cli);

        assertArgumentsPresentInOrder(cli, "-D", "key=value");
    }

    @Test
    void shouldSpecifyCustomPropertyWithSpacesInValueFromRequest() {

        Properties properties = new Properties();
        properties.setProperty("key", "value with spaces");

        mclb.setProperties(newRequest().setProperties(properties), cli);

        assertArgumentsPresentInOrder(cli, "-D", "key=value with spaces");
    }

    @Test
    void shouldSpecifyCustomPropertyWithSpacesInKeyFromRequest() {

        Properties properties = new Properties();
        properties.setProperty("key with spaces", "value with spaces");

        mclb.setProperties(newRequest().setProperties(properties), cli);

        assertArgumentsPresentInOrder(cli, "-D", "key with spaces=value with spaces");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldSpecifySingleGoalFromRequest() throws Exception {

        List<String> goals = new ArrayList<>();
        goals.add("test");

        mclb.setGoals(newRequest().setGoals(goals), cli);

        assertArgumentsPresent(cli, Collections.singleton("test"));
    }

    @Test
    void shouldSpecifySingleGoalFromRequestArg() throws Exception {

        mclb.setArgs(newRequest().addArg("test"), cli);

        assertArgumentsPresent(cli, Collections.singleton("test"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldSpecifyTwoGoalsFromRequest() throws Exception {
        List<String> goals = new ArrayList<>();
        goals.add("test");
        goals.add("clean");

        mclb.setGoals(newRequest().setGoals(goals), cli);

        assertArgumentsPresent(cli, new HashSet<>(goals));
        assertArgumentsPresentInOrder(cli, goals);
    }

    @Test
    void shouldSpecifyTwoGoalsFromRequestArgs() throws Exception {
        List<String> goals = new ArrayList<>();
        goals.add("test");
        goals.add("clean");

        mclb.setArgs(newRequest().addArgs(goals), cli);

        assertArgumentsPresent(cli, new HashSet<>(goals));
        assertArgumentsPresentInOrder(cli, goals);
    }

    @Test
    void shouldSpecifyThreadsFromRequest() {
        mclb.setThreads(newRequest().setThreads("2.0C"), cli);

        assertArgumentsPresentInOrder(cli, "-T", "2.0C");
    }

    @Test
    void buildTypicalMavenInvocationEndToEnd() throws Exception {
        File mavenDir = setupTempMavenHomeIfMissing(false);

        InvocationRequest request = newRequest();

        File projectDir = Files.createDirectories(
                        temporaryFolder.resolve("invoker-tests").resolve("typical-end-to-end-cli-build"))
                .toFile();

        request.setBaseDirectory(projectDir);

        Set<String> expectedArgs = new HashSet<>();
        Set<String> bannedArgs = new HashSet<>();

        createDummyFile(projectDir, "pom.xml");

        bannedArgs.add("-f");
        bannedArgs.add("pom.xml");

        Properties properties = new Properties();
        // this is REALLY bad practice, but since it's just a test...
        properties.setProperty("maven.tests.skip", "true");

        expectedArgs.add("maven.tests.skip=true");

        request.setProperties(properties);

        request.setOffline(true);

        expectedArgs.add("-o");

        List<String> goals = new ArrayList<>();

        goals.add("post-clean");
        goals.add("deploy");
        goals.add("site-deploy");

        request.addArgs(goals);

        Commandline commandline = mclb.build(request);

        assertArgumentsPresent(commandline, expectedArgs);
        assertArgumentsNotPresent(commandline, bannedArgs);
        assertArgumentsPresentInOrder(commandline, goals);

        String executable = commandline.getExecutable();

        assertTrue(executable.contains(new File(mavenDir, "bin/mvn").getCanonicalPath()));
        assertEquals(
                projectDir.getCanonicalPath(), commandline.getWorkingDirectory().getCanonicalPath());
    }

    @Test
    void shouldInsertActivatedProfiles() throws Exception {
        setupTempMavenHomeIfMissing(false);

        String profile1 = "profile-1";
        String profile2 = "profile-2";

        InvocationRequest request = newRequest();

        List<String> profiles = new ArrayList<>();
        profiles.add(profile1);
        profiles.add(profile2);

        request.setProfiles(profiles);

        Commandline commandline = mclb.build(request);

        assertArgumentsPresentInOrder(commandline, "-P", profile1 + "," + profile2);
    }

    @Test
    void mvnExecutableFromInvoker() throws Exception {
        assumeTrue(Objects.nonNull(System.getProperty("maven.home")), "Test only works when maven.home is set");

        File mavenExecutable = new File("mvnDebug");

        mclb.setMavenExecutable(mavenExecutable);
        mclb.build(newRequest());

        assertTrue(mclb.getMavenExecutable().exists(), "Expected executable to exist");
        assertTrue(mclb.getMavenExecutable().isAbsolute(), "Expected executable to be absolute");
        assertTrue(mclb.getMavenExecutable().getName().contains("mvnDebug"), "Expected mvnDebug as command mvnDebug");
    }

    @Test
    void mvnExecutableFormRequest() throws Exception {
        assumeTrue(Objects.nonNull(System.getProperty("maven.home")), "Test only works when maven.home is set");

        File mavenExecutable = new File("mvnDebug");

        mclb.build(newRequest().setMavenExecutable(mavenExecutable));

        assertTrue(mclb.getMavenExecutable().exists(), "Expected executable to exist");
        assertTrue(mclb.getMavenExecutable().isAbsolute(), "Expected executable to be absolute");
        assertTrue(mclb.getMavenExecutable().getName().contains("mvnDebug"), "Expected mvnDebug as command");
    }

    @Test
    void defaultMavenCommand() throws Exception {
        assumeTrue(Objects.nonNull(System.getProperty("maven.home")), "Test only works when maven.home is set");

        mclb.build(newRequest());

        assertTrue(mclb.getMavenExecutable().exists(), "Expected executable to exist");
        assertTrue(mclb.getMavenExecutable().isAbsolute(), "Expected executable to be absolute");
    }

    @Test
    void addShellEnvironment() throws Exception {
        setupTempMavenHomeIfMissing(false);

        InvocationRequest request = newRequest();

        String envVar1Name = "VAR-1";
        String envVar1Value = "VAR-1-VALUE";

        String envVar2Name = "VAR-2";
        String envVar2Value = "VAR-2-VALUE";

        request.addShellEnvironment(envVar1Name, envVar1Value);
        request.addShellEnvironment(envVar2Name, envVar2Value);

        Commandline commandline = mclb.build(request);

        assertEnvironmentVariablePresent(commandline, envVar1Name, envVar1Value);
        assertEnvironmentVariablePresent(commandline, envVar2Name, envVar2Value);
    }

    private void assertEnvironmentVariablePresent(Commandline cli, String varName, String varValue) {
        List<String> environmentVariables = Arrays.asList(cli.getEnvironmentVariables());

        String expectedDeclaration = varName + "=" + varValue;

        assertTrue(
                environmentVariables.contains(expectedDeclaration),
                "Environment variable setting: '" + expectedDeclaration + "' is missing in " + environmentVariables);
    }

    private void assertArgumentsPresentInOrder(Commandline cli, String... expected) {
        assertArgumentsPresentInOrder(cli, Arrays.asList(expected));
    }

    private void assertArgumentsPresentInOrder(Commandline cli, List<String> expected) {
        String[] arguments = cli.getArguments();

        int expectedCounter = 0;

        for (String argument : arguments) {
            if (argument.equals(expected.get(expectedCounter))) {
                expectedCounter++;
            }
        }

        assertEquals(
                expected.size(),
                expectedCounter,
                "Arguments: " + expected + " were not found or are in the wrong order: " + Arrays.asList(arguments));
    }

    private void assertArgumentsPresent(Commandline cli, Set<String> requiredArgs) {
        String[] argv = cli.getArguments();
        List<String> args = Arrays.asList(argv);

        for (String arg : requiredArgs) {
            assertTrue(args.contains(arg), "Command-line argument: '" + arg + "' is missing in " + args);
        }
    }

    private void assertArgumentsNotPresent(Commandline cli, Set<String> bannedArgs) {
        String[] argv = cli.getArguments();
        List<String> args = Arrays.asList(argv);

        for (String arg : bannedArgs) {
            assertFalse(args.contains(arg), "Command-line argument: '" + arg + "' should not be present.");
        }
    }

    private File createDummyFile(File directory, String filename) throws IOException {
        File dummyFile = new File(directory, filename);

        try (FileWriter writer = new FileWriter(dummyFile)) {
            writer.write("This is a dummy file.");
        }

        return dummyFile;
    }

    private InvocationRequest newRequest() {
        return new DefaultInvocationRequest();
    }
}
