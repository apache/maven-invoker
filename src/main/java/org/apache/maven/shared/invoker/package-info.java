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

/**
 * <p><strong>DEPRECATED:</strong> This package is deprecated and will be replaced by
 * <a href="https://github.com/apache/maven/tree/master/impl/maven-executor">maven-executor</a>.</p>
 *
 * <p>The {@code maven-invoker} component provides an API for programmatically invoking Maven builds.
 * However, it is being replaced by {@code maven-executor} which offers significant improvements:</p>
 *
 * <h2>Why Migrate to maven-executor?</h2>
 * <ul>
 *   <li><strong>Unified and Simpler API:</strong> No need for updates when Maven CLI changes</li>
 *   <li><strong>Modern Maven Support:</strong> Works with both Maven 3.9+ and Maven 4+</li>
 *   <li><strong>Flexible Execution:</strong> Supports both forked and embedded execution modes</li>
 *   <li><strong>Better Isolation:</strong> Proper environment and system property isolation</li>
 *   <li><strong>Zero Dependencies:</strong> Completely standalone library</li>
 *   <li><strong>Actively Maintained:</strong> Used in Maven 4 Integration Tests</li>
 * </ul>
 *
 * <h2>Migration</h2>
 * <p>For detailed migration instructions, including code examples and API comparisons, see the
 * <a href="https://maven.apache.org/shared/maven-invoker/migration.html">Migration Guide</a>.</p>
 *
 * <h2>Quick Example</h2>
 * <p>Old code (maven-invoker):</p>
 * <pre>{@code
 * InvocationRequest request = new DefaultInvocationRequest();
 * request.setPomFile(new File("/path/to/pom.xml"));
 * request.setGoals(Collections.singletonList("install"));
 *
 * Invoker invoker = new DefaultInvoker();
 * InvocationResult result = invoker.execute(request);
 * }</pre>
 *
 * <p>New code (maven-executor):</p>
 * <pre>{@code
 * ExecutorRequest request = ExecutorRequest.builder()
 *     .cwd(Paths.get("/path/to"))
 *     .command("mvn")
 *     .args(new String[]{"install"})
 *     .build();
 *
 * ForkedExecutor executor = new ForkedExecutor();
 * int exitCode = executor.execute(request);
 * }</pre>
 *
 * <h2>Support</h2>
 * <ul>
 *   <li><a href="https://github.com/apache/maven-invoker/issues/164">Issue #164 - Deprecation Announcement</a></li>
 *   <li><a href="https://github.com/apache/maven/tree/master/impl/maven-executor">maven-executor Source Code</a></li>
 *   <li><a href="https://maven.apache.org/mailing-lists.html">Maven Mailing Lists</a></li>
 * </ul>
 *
 * @deprecated Use <a href="https://github.com/apache/maven/tree/master/impl/maven-executor">maven-executor</a>
 *             instead. This package will be removed in a future version.
 */
package org.apache.maven.shared.invoker;
