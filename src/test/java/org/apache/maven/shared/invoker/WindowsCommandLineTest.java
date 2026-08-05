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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@EnabledOnOs(OS.WINDOWS)
@SuppressWarnings("deprecation")
class WindowsCommandLineTest {

    private static final String TEST_COMMAND_RESOURCE = "/test-command-line/test.cmd";

    private static final List<String> ARGUMENTS = Arrays.asList(
            "ampersand & argument",
            "pipe | argument",
            "less < argument",
            "greater > argument",
            "left ( argument",
            "right ) argument",
            "caret ^ argument");

    private static final List<String> EXPECTED_ARGUMENT_OUTPUT = Arrays.asList(
            "1: [\"-B\"]",
            "2: [\"ampersand & argument\"]",
            "3: [\"pipe | argument\"]",
            "4: [\"less < argument\"]",
            "5: [\"greater > argument\"]",
            "6: [\"left ( argument\"]",
            "7: [\"right ) argument\"]",
            "8: [\"caret ^ argument\"]");

    @TempDir
    private Path temporaryDirectory;

    @TestFactory
    List<DynamicTest> testCmdExecutablePaths() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();

        for (String directoryName : Arrays.asList(
                "plain", "two words", "two(1)words", "two (1) words", "two&words", "two^words", "two & (1) ^ words")) {
            Path command = copyTestCommand(directoryName);

            tests.add(dynamicTest(directoryName + " - relative", () -> execute(command, false)));
            tests.add(dynamicTest(directoryName + " - absolute", () -> execute(command, true)));
        }

        return tests;
    }

    private Path copyTestCommand(String directoryName) throws IOException {
        Path directory = Files.createDirectories(temporaryDirectory.resolve(directoryName));
        Path command = directory.resolve("test.cmd");

        try (InputStream input = WindowsCommandLineTest.class.getResourceAsStream(TEST_COMMAND_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + TEST_COMMAND_RESOURCE);
            }
            Files.copy(input, command, StandardCopyOption.REPLACE_EXISTING);
        }

        return command;
    }

    private void execute(Path command, boolean absolute) throws IOException, MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(temporaryDirectory.toFile());
        request.setBatchMode(true);
        request.setMavenExecutable(
                absolute ? command.toFile().getAbsoluteFile() : relativeCommandWithoutExtension(command));
        request.addArgs(ARGUMENTS);

        List<String> output = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        request.setOutputHandler(output::add);
        request.setErrorHandler(errors::add);

        InvocationResult result = new DefaultInvoker().execute(request);

        assertNull(result.getExecutionException(), () -> "Execution failed; stderr: " + errors);
        assertEquals(0, result.getExitCode(), () -> "Unexpected exit code; stderr: " + errors);
        assertEquals(expectedOutput(command), output);
    }

    private List<String> expectedOutput(Path command) throws IOException {
        String commandPath = command.toFile().getCanonicalPath();

        List<String> expectedOutput = new ArrayList<>();
        expectedOutput.add("0: [\"" + commandPath + "\"]");
        expectedOutput.addAll(EXPECTED_ARGUMENT_OUTPUT);
        return expectedOutput;
    }

    private File relativeCommandWithoutExtension(Path command) {
        String relativeCommand = temporaryDirectory.relativize(command).toString();
        return new File(relativeCommand.substring(0, relativeCommand.length() - ".cmd".length()));
    }
}
