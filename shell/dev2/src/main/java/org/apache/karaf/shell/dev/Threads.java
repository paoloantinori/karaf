/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.dev;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "dev", name = "threads", description = "Show threads in the JVM.")
public class Threads extends OsgiCommandSupport {

    @Option(name = "-f", aliases = { "--flat" }, description = "Do not display threads as a tree")
    private boolean flat;

    @Override
    protected Object doExecute() throws Exception {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        print(group, "");
        return null;
    }
    
    void print(ThreadGroup group, String indent) {
        if (!flat) {
            System.out.println(indent + "Thread Group \"" + group.getName() + "\"");
        }

        int nb;

        ThreadGroup[] childGroups = new ThreadGroup[32];
        while (true) {
            nb = group.enumerate(childGroups, false);
            if (nb == childGroups.length) {
                childGroups = new ThreadGroup[childGroups.length * 2];
            } else {
                break;
            }
        }
        for (int i = 0; i < nb; i++) {
            print(childGroups[i], indent + (flat ? "" : "    "));
        }

        Thread[] childThreads = new Thread[32];
        while (true) {
            nb = group.enumerate(childThreads, false);
            if (nb == childThreads.length) {
                childThreads = new Thread[childThreads.length * 2];
            } else {
                break;
            }
        }
        for (int i = 0; i < nb; i++) {
            System.out.println(indent + (flat ? "" : "    ") + "\"" + childThreads[i].getName() + "\": " + childThreads[i].getState());
        }
    }

}
