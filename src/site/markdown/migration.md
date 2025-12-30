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

The `maven-invoker` component is deprecated and replaced by [maven-executor](https://github.com/apache/maven/tree/master/impl/maven-executor), which provides a modern, unified API for programmatically invoking Maven builds.

## Why Migrate?

`maven-executor` offers several advantages:

- Unified API: Simple, consistent API that doesn't require updates when Maven CLI changes
- Maven 3.9+ and Maven 4+ Support: Works seamlessly with both Maven versions
- Multiple Execution Modes: Supports both forked and embedded execution
- Better Environment Isolation: Proper isolation of environment variables and system properties
- Zero Dependencies: Completely standalone, no additional dependencies required
- Maintained: Actively used in Maven 4 Integration Tests
## Dependency Changes

### Old (maven-invoker)

```xml
<dependency>
    <groupId>org.apache.maven.shared</groupId>
    <artifactId>maven-invoker</artifactId>
</dependency>
```

### New (maven-executor)

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-executor</artifactId>
    <version>4.0.0-rc5</version>
</dependency>
```

**Note:** Since maven-executor is part of Maven 4, check the latest Maven 4 releases for current version numbers.

## API Comparison

### Basic Invocation

Old Code (maven-invoker):

```java
InvocationRequest request = new DefaultInvocationRequest();
request.setPomFile(new File("/path/to/pom.xml"));
request.setGoals(Collections.singletonList("install"));

Invoker invoker = new DefaultInvoker();
InvocationResult result = invoker.execute(request);

if (result.getExitCode() != 0) {
    throw new IllegalStateException("Build failed.");
}
```

New Code (maven-executor):

```java
ExecutorRequest request = ExecutorRequest.builder()
    .cwd(Paths.get("/path/to"))
    .command("mvn")
    .args(new String[]{"install"})
    .build();

ForkedExecutor executor = new ForkedExecutor();
int exitCode = executor.execute(request);

if (exitCode != 0) {
    throw new IllegalStateException("Build failed.");
}
```

### Setting Goals and Options

Old Code (maven-invoker):

```java
InvocationRequest request = new DefaultInvocationRequest();
request.setPomFile(new File("/path/to/pom.xml"));
request.setGoals(Arrays.asList("clean", "install"));
request.setBatchMode(true);
request.setOffline(true);
request.setDebug(true);
```

New Code (maven-executor):

```java
ExecutorRequest request = ExecutorRequest.builder()
    .cwd(Paths.get("/path/to"))
    .command("mvn")
    .args(new String[]{
        "clean", "install",
        "--batch-mode",
        "--offline",
        "--debug"
    })
    .build();
```

### Embedded Execution

maven-invoker only supports forked execution, while maven-executor supports both.

```java
import org.apache.maven.cling.executor.EmbeddedExecutor;

EmbeddedExecutor executor = new EmbeddedExecutor();
ExecutorRequest request = ExecutorRequest.builder()
    .cwd(Paths.get("/path/to/project"))
    .args(new String[]{"install"})
    .build();

int exitCode = executor.execute(request);
```

## Complete Migration Example

Before (maven-invoker):

```java
import org.apache.maven.shared.invoker.*;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;

public class MavenBuildRunner {
    private final Invoker invoker;

    public MavenBuildRunner(File localRepo) {
        Invoker newInvoker = new DefaultInvoker();
        newInvoker.setLocalRepositoryDirectory(localRepo);
        this.invoker = newInvoker;
    }

    public void runBuild(File projectDir) throws Exception {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(projectDir);
        request.setGoals(Arrays.asList("clean", "install"));
        request.setBatchMode(true);

        Properties props = new Properties();
        props.setProperty("skipTests", "false");
        request.setProperties(props);

        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0) {
            throw new Exception("Build failed");
        }
    }
}
```

After (maven-executor):

```java
import org.apache.maven.api.cli.*;
import org.apache.maven.cling.executor.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildRunner {
    private final ForkedExecutor executor;
    private final Path localRepo;

    public MavenBuildRunner(Path localRepo) {
        this.executor = new ForkedExecutor();
        this.localRepo = localRepo;
    }

    public void runBuild(Path projectDir) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("clean");
        args.add("install");
        args.add("--batch-mode");
        args.add("-Dmaven.repo.local=" + localRepo.toString());
        args.add("-DskipTests=false");

        ExecutorRequest request = ExecutorRequest.builder()
            .cwd(projectDir)
            .command("mvn")
            .args(args.toArray(new String[0]))
            .build();

        int exitCode = executor.execute(request);

        if (exitCode != 0) {
            throw new Exception("Build failed");
        }
    }
}
```

## Additional Resources

- [maven-executor source code](https://github.com/apache/maven/tree/master/impl/maven-executor)
- [Issue #164 - Deprecation announcement](https://github.com/apache/maven-invoker/issues/164)
- [Related discussion in maven-verifier](https://github.com/apache/maven-verifier/issues/186)
- [Maven developer mailing list](https://maven.apache.org/mailing-lists.html)
- [Detailed Migration Guide (MIGRATION.md)](./MIGRATION.md)
## Need Help?

If you encounter migration issues or have questions:

1. Check the [maven-executor source code](https://github.com/apache/maven/tree/master/impl/maven-executor) for examples
1. Review [Maven 4 IT tests](https://github.com/apache/maven/tree/master/its) that use maven-executor
1. Open an issue on the [maven-invoker repository](https://github.com/apache/maven-invoker/issues)
1. Contact the [Maven developer mailing list](https://maven.apache.org/mailing-lists.html)
