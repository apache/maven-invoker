<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Migration from maven-invoker to maven-executor

## Overview

The `maven-invoker` component is deprecated and replaced by [`maven-executor`](https://github.com/apache/maven/tree/master/impl/maven-executor), which provides a modern, unified API for programmatically invoking Maven builds.

## Why Migrate?

**maven-executor** offers several advantages:

- **Unified API**: Simple, consistent API that doesn't require updates when Maven CLI changes
- **Maven 3.9+ and Maven 4+ Support**: Works seamlessly with both Maven versions
- **Multiple Execution Modes**: Supports both forked and embedded execution
- **Better Environment Isolation**: Proper isolation of environment variables and system properties
- **Zero Dependencies**: Completely standalone, no additional dependencies required
- **Maintained**: Actively used in Maven 4 Integration Tests

## Dependency Changes

### Old (maven-invoker)

```xml
<dependency>
    <groupId>org.apache.maven.shared</groupId>
    <artifactId>maven-invoker</artifactId>
</dependency>
```

### New (maven-executor)

**Option 1: Using Maven 4+ (recommended)**

```xml
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-executor</artifactId>
    <version>4.0.0-rc5</version>
</dependency>
```

> **Note**: Check the latest Maven 4 releases for current version numbers.

## API Comparison and Migration Examples

### Basic Invocation

#### Old Code (maven-invoker)

```java
import org.apache.maven.shared.invoker.*;
import java.io.File;
import java.util.Collections;

InvocationRequest request = new DefaultInvocationRequest();
request.setPomFile(new File("/path/to/pom.xml"));
request.setGoals(Collections.singletonList("install"));

Invoker invoker = new DefaultInvoker();
InvocationResult result = invoker.execute(request);

if (result.getExitCode() != 0) {
    throw new IllegalStateException("Build failed.");
}
```

#### New Code (maven-executor)

```java
import org.apache.maven.api.cli.*;
import org.apache.maven.cling.executor.*;
import java.nio.file.Path;
import java.nio.file.Paths;

// Using forked executor
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

#### Old Code (maven-invoker)

```java
InvocationRequest request = new DefaultInvocationRequest();
request.setPomFile(new File("/path/to/pom.xml"));
request.setGoals(Arrays.asList("clean", "install"));
request.setBatchMode(true);
request.setOffline(true);
request.setDebug(true);
request.setProperties(properties);
```

#### New Code (maven-executor)

```java
ExecutorRequest request = ExecutorRequest.builder()
    .cwd(Paths.get("/path/to"))
    .command("mvn")
    .args(new String[]{
        "clean", "install",
        "--batch-mode",
        "--offline",
        "--debug",
        "-Dproperty1=value1",
        "-Dproperty2=value2"
    })
    .build();
```

### Maven Home Configuration

#### Old Code (maven-invoker)

```java
Invoker invoker = new DefaultInvoker();
invoker.setMavenHome(new File("/path/to/maven"));
```

#### New Code (maven-executor)

```java
// Maven home is automatically detected from MAVEN_HOME or M2_HOME
// or can be specified via command path
ExecutorRequest request = ExecutorRequest.builder()
    .command("/path/to/maven/bin/mvn")
    .args(new String[]{"install"})
    .build();
```

### Local Repository Configuration

#### Old Code (maven-invoker)

```java
InvocationRequest request = new DefaultInvocationRequest();
request.setLocalRepositoryDirectory(new File("/path/to/local/repo"));
```

#### New Code (maven-executor)

```java
ExecutorRequest request = ExecutorRequest.builder()
    .args(new String[]{
        "install",
        "-Dmaven.repo.local=/path/to/local/repo"
    })
    .build();
```

### Capturing Output

#### Old Code (maven-invoker)

```java
InvocationOutputHandler outputHandler = new InvocationOutputHandler() {
    @Override
    public void consumeLine(String line) {
        System.out.println(line);
    }
};

InvocationRequest request = new DefaultInvocationRequest();
request.setOutputHandler(outputHandler);
```

#### New Code (maven-executor)

```java
// Output handling through standard Java ProcessBuilder mechanisms
ProcessBuilder.Redirect redirect = ProcessBuilder.Redirect.PIPE;

// Or use custom output streams
ExecutorRequest request = ExecutorRequest.builder()
    .args(new String[]{"install"})
    .outputRedirect(yourOutputStream)
    .build();
```

### Embedded Execution

#### Old Code (maven-invoker)

```java
// maven-invoker only supports forked execution
```

#### New Code (maven-executor)

```java
// maven-executor supports embedded execution!
import org.apache.maven.cling.executor.EmbeddedExecutor;

EmbeddedExecutor executor = new EmbeddedExecutor();
ExecutorRequest request = ExecutorRequest.builder()
    .cwd(Paths.get("/path/to/project"))
    .args(new String[]{"install"})
    .build();

int exitCode = executor.execute(request);
```

### Environment Variables

#### Old Code (maven-invoker)

```java
InvocationRequest request = new DefaultInvocationRequest();
request.setShellEnvironmentInherited(true);
// or
request.addShellEnvironment("MY_VAR", "value");
```

#### New Code (maven-executor)

```java
Map<String, String> env = new HashMap<>();
env.put("MY_VAR", "value");

ExecutorRequest request = ExecutorRequest.builder()
    .args(new String[]{"install"})
    .environment(env)
    .build();
```

## Complete Migration Checklist

- [ ] Update dependencies in `pom.xml`
- [ ] Replace `InvocationRequest` with `ExecutorRequest`
- [ ] Replace `Invoker` with `ForkedExecutor` or `EmbeddedExecutor`
- [ ] Convert file paths from `File` to `Path`/`Paths`
- [ ] Convert goals and options to command-line arguments array
- [ ] Update output handling mechanism
- [ ] Update environment variable handling
- [ ] Update system property handling
- [ ] Update test configurations
- [ ] Verify Maven home detection or configuration
- [ ] Test with both Maven 3.9+ and Maven 4+ if needed

## Key Differences Summary

| Feature | maven-invoker | maven-executor |
|---------|--------------|----------------|
| API Style | Request/Invoker objects | Builder pattern with args array |
| Maven Versions | Maven 2.x, 3.x | Maven 3.9+, 4+ |
| Execution Modes | Forked only | Forked and Embedded |
| Dependencies | Has dependencies | Zero dependencies |
| CLI Mapping | Manual mapping in code | Direct CLI args |
| Maintenance | Requires updates for CLI changes | No updates needed for CLI changes |

## Additional Resources

- [maven-executor source code](https://github.com/apache/maven/tree/master/impl/maven-executor)
- [maven-executor CLI API](https://github.com/apache/maven/tree/master/impl/maven-executor/src/main/java/org/apache/maven/api/cli)
- [maven-executor implementation](https://github.com/apache/maven/tree/master/impl/maven-executor/src/main/java/org/apache/maven/cling/executor)
- [Maven 4 Integration Tests using maven-executor](https://github.com/apache/maven/blob/master/its/core-it-support/maven-it-helper/src/main/java/org/apache/maven/it/Verifier.java)
- [Issue #164 - Deprecation announcement](https://github.com/apache/maven-invoker/issues/164)
- [Related discussion in maven-verifier](https://github.com/apache/maven-verifier/issues/186)

## Need Help?

If you encounter migration issues or have questions:

1. Check the [maven-executor source code](https://github.com/apache/maven/tree/master/impl/maven-executor) for examples
2. Review [Maven 4 IT tests](https://github.com/apache/maven/tree/master/its) that use maven-executor
3. Open an issue on the [maven-invoker repository](https://github.com/apache/maven-invoker/issues) for migration-specific questions
4. Contact the [Maven developer mailing list](https://maven.apache.org/mailing-lists.html)

