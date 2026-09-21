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
import java.io.FileOutputStream;

import org.junit.Test;

public class AppTest
{
    /**
     * Never ends on its own: the invoker's timeout has to stop this forked JVM. The heartbeat file lets the
     * invoker test see whether it did.
     */
    @Test
    public void testApp() throws Exception {
        File heartbeat = new File( System.getProperty( "heartbeat" ) );
        while (true) {
            try ( FileOutputStream out = new FileOutputStream( heartbeat, true ) ) {
                out.write( '.' );
            }
            Thread.sleep( 200L );
        }
    }
}
