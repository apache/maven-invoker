<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Migration from maven-invoker to maven-executor

## Overview

The `maven-invoker` component is deprecated and replaced by [maven-executor](https://github.com/apache/maven-executor), a standalone library that runs Maven 3.9+ and Maven 4 programmatically, forked or embedded. Its only artifact, `org.apache.maven.executor:maven-executor`, has no dependencies and is built for Java 8, so it fits plugins on the 3.x line as well as Maven 4 code.

## Dependency changes

Old:

```xml
<dependency>
  <groupId>org.apache.maven.shared</groupId>
  <artifactId>maven-invoker</artifactId>
</dependency>
```

New:

```xml
<dependency>
  <groupId>org.apache.maven.executor</groupId>
  <artifactId>maven-executor</artifactId>
  <version>1.0.0</version>
</dependency>
```

Check [Maven Central](https://central.sonatype.com/artifact/org.apache.maven.executor/maven-executor) for the current version.

## What changes

maven-invoker models the Maven command line as setters on `InvocationRequest` (`setGoals`, `setDebug`, `setOffline`, ...) and turns them into arguments for the `mvn` script. maven-executor takes the arguments as you would type them, and adds what a script cannot express: the working directory, the environment, JVM system properties, the output streams, and a timeout. In practice a migration replaces each setter with the equivalent CLI option and moves the process settings to the request builder.

| maven-invoker | maven-executor |
|---|---|
| `Invoker invoker = new DefaultInvoker(); invoker.setMavenHome(dir)` | `new ForkedMavenExecutor(dir.toPath())` or `new EmbeddedMavenExecutor(dir.toPath())`; `ExecutorRequest.discoverInstallationDirectory()` reads `maven.home` |
| `request.setBaseDirectory(dir)` | `.cwd(dir.toPath())` |
| `request.setPomFile(file)` | `.argument("-f").argument(file.getPath())`, or `.cwd(...)` on the project directory |
| `request.setGoals(list)` | `.arguments(list)` |
| `request.setProfiles(list)` | `.argument("-P" + String.join(",", list))` |
| `request.setBatchMode(true)`, `setOffline(true)`, `setDebug(true)`, `setQuiet(true)`, `setShowErrors(true)`, `setShowVersion(true)`, `setUpdateSnapshots(true)` | `.argument("-B")`, `"-o"`, `"-X"`, `"-q"`, `"-e"`, `"-V"`, `"-U"` |
| `request.setProperties(props)` | one `.argument("-Dkey=value")` per property |
| `request.setUserSettingsFile(file)`, `setGlobalSettingsFile(file)`, `setToolchainsFile(file)` | `.argument("-s").argument(path)`, `"-gs"`, `"-t"` |
| `request.setLocalRepositoryDirectory(dir)` | `.argument("-Dmaven.repo.local=" + dir)` |
| `request.setThreads("4")` | `.argument("-T").argument("4")` |
| `request.setReactorFailureBehavior(...)` | `.argument("--fail-at-end")`, `"--fail-fast"`, `"--fail-never"` |
| `request.setJavaHome(dir)` | `.environmentVariable("JAVA_HOME", dir.getPath())` (forked) |
| `request.setMavenOpts(opts)` | `.environmentVariable("MAVEN_OPTS", opts)` (forked), or `.jvmArgument(...)` |
| `request.addShellEnvironment(k, v)` | `.environmentVariable(k, v)` |
| `request.setOutputHandler(handler)`, `setErrorHandler(handler)` | `.stdOut(stream)`, `.stdErr(stream)`; or leave both unset and read `result.stdOutString()` |
| `request.setInputStream(in)` | `.stdIn(in)` |
| `request.setTimeoutInSeconds(n)` | `.executionTimeout(Duration.ofSeconds(n))` |
| `request.setMavenExecutable(file)` | `.command("mvnDebug")` for the sibling scripts of the installation; another executable is not supported |
| `result.getExitCode()`, `result.getExecutionException()` | `result.exitCode()` (an `Optional<Integer>`), `result.success()`; failures to start Maven throw `ExecutorException` |

## Examples

### Run goals in a project

Old:

```java
import org.apache.maven.shared.invoker.*;

InvocationRequest request = new DefaultInvocationRequest();
request.setBaseDirectory(new File("/path/to/project"));
request.setGoals(Arrays.asList("clean", "install"));
request.setBatchMode(true);

Invoker invoker = new DefaultInvoker();
invoker.setMavenHome(new File("/path/to/maven"));
InvocationResult result = invoker.execute(request);
if (result.getExitCode() != 0) {
    throw new IllegalStateException("Build failed.");
}
```

New:

```java
import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.ExecutorResult;
import org.apache.maven.executor.forked.ForkedMavenExecutor;

ExecutorRequest request = ExecutorRequest.mavenBuilder()
        .cwd(Paths.get("/path/to/project"))
        .arguments("-B", "clean", "install")
        .build();

try (ForkedMavenExecutor executor = new ForkedMavenExecutor(Paths.get("/path/to/maven"))) {
    ExecutorResult result = executor.execute(request);
    if (!result.success()) {
        throw new IllegalStateException("Build failed: " + result.stdOutString().orElse(""));
    }
}
```

`ExecutorRequest.mavenBuilder()` starts from the current working directory and user home, and captures the output as a string unless you set streams.

### Options and properties

Old:

```java
request.setOffline(true);
request.setDebug(true);
request.setProfiles(Arrays.asList("ci", "fast"));
request.setUserSettingsFile(settings);
request.setLocalRepositoryDirectory(repo);
Properties props = new Properties();
props.setProperty("skipTests", "true");
request.setProperties(props);
```

New:

```java
ExecutorRequest request = ExecutorRequest.mavenBuilder()
        .cwd(projectDir)
        .arguments("-o", "-X", "-Pci,fast", "-s", settings.toString(), "-Dmaven.repo.local=" + repo, "-DskipTests=true", "verify")
        .build();
```

### Stream the output

Old:

```java
request.setOutputHandler(line -> log.info(line));
request.setErrorHandler(line -> log.error(line));
```

New:

```java
try (OutputStream out = Files.newOutputStream(logFile)) {
    ExecutorRequest request = ExecutorRequest.mavenBuilder()
            .cwd(projectDir)
            .arguments("verify")
            .stdOut(out)
            .stdErr(out)
            .build();
    executor.execute(request);
}
```

Maven's log and the transfer messages go to `stdOut`; `stdErr` carries what the JVM prints before Maven starts. A line-oriented handler becomes an `OutputStream` that splits on line ends.

### Embedded execution

maven-invoker only forks. maven-executor can run Maven inside the current JVM, which is faster for many short builds:

```java
import org.apache.maven.executor.embedded.EmbeddedMavenExecutor;

try (EmbeddedMavenExecutor executor = new EmbeddedMavenExecutor(installation)) {
    executor.execute(request);
}
```

`ExecutorHelper.forMavenInstallation(installation, ExecutorHelper.Mode.AUTO)` picks the embedded executor unless the request needs a fork (environment variables or JVM arguments). Embedded runs share the JVM, so a build that sets system properties or leaks threads affects the caller.

### Environment and JVM

Old:

```java
request.setJavaHome(new File("/opt/jdk17"));
request.setMavenOpts("-Xmx1g");
request.addShellEnvironment("CI", "true");
```

New:

```java
ExecutorRequest request = ExecutorRequest.mavenBuilder()
        .cwd(projectDir)
        .arguments("verify")
        .environmentVariable("JAVA_HOME", "/opt/jdk17")
        .environmentVariable("MAVEN_OPTS", "-Xmx1g")
        .environmentVariable("CI", "true")
        .skipMavenRc(true)
        .build();
```

The forked executor starts from a clean environment, so pass what the build needs; `skipMavenRc(true)` keeps the user's `.mavenrc` out of the run.

## Migration checklist

- Replace the dependency and the `org.apache.maven.shared.invoker` imports.
- Replace each `InvocationRequest` setter with the CLI option from the table.
- Move Maven home from `Invoker.setMavenHome` to the executor constructor; in tests run under Surefire or Failsafe, pass `maven.home` explicitly through `systemPropertyVariables`, the forked JVM does not inherit it.
- Replace the output handlers with streams, or read `stdOutString()`.
- Check the exit code through `ExecutorResult.success()` and catch `ExecutorException` for a Maven that could not start.

## Resources

- [maven-executor repository](https://github.com/apache/maven-executor)
- [Maven 4 integration tests](https://github.com/apache/maven/tree/master/its), the first consumer of the API
- [Issue #164, the deprecation](https://github.com/apache/maven-invoker/issues/164)
- [maven-verifier's migration guide](https://github.com/apache/maven-verifier/blob/master/MIGRATION.md), for the verifier side of the same move
