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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "dev", name = "threads", description = "Show threads in the JVM.")
public class Threads extends OsgiCommandSupport {

    @Option(name = "-d", aliases = { "--dump" }, description = "Display thread stack traces")
    private boolean dump;

    @Option(name = "-f", aliases = { "--flat" }, description = "Do not display threads as a tree")
    private boolean flat;

    @Option(name = "-e", aliases = { "--empty-groups" }, description = "Show empty groups")
    private boolean empty;

    @Option(name = "-p", aliases = { "--prune" }, description = "Prune uninteresting threads")
    private boolean prune;

    @Override
    protected Object doExecute() throws Exception {
        Map<Long, ThreadInfo> threadInfos = new HashMap<Long, ThreadInfo>();
        if (dump) {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] infos;
            if (threadMXBean.isObjectMonitorUsageSupported() && threadMXBean.isSynchronizerUsageSupported()) {
                infos = threadMXBean.dumpAllThreads(true, true);
            } else {
                infos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE);
            }
            for (ThreadInfo info : infos) {
                threadInfos.put(info.getThreadId(), info);
            }
            flat = true;
        }

        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        new ThreadGroupData(group, threadInfos).print("");
        return null;
    }

    public class ThreadGroupData {
        private final ThreadGroup group;
        private final List<ThreadGroupData> groups;
        private final List<ThreadData> threads;

        public ThreadGroupData(ThreadGroup group, Map<Long, ThreadInfo> infos) {
            this.group = group;
            int nbGroups;
            int nbThreads;
            ThreadGroup[] childGroups = new ThreadGroup[32];
            while (true) {
                nbGroups = group.enumerate(childGroups, false);
                if (nbGroups == childGroups.length) {
                    childGroups = new ThreadGroup[childGroups.length * 2];
                } else {
                    break;
                }
            }
            groups = new ArrayList<ThreadGroupData>();
            for (ThreadGroup tg : childGroups) {
                if (tg != null) {
                    groups.add(new ThreadGroupData(tg, infos));
                }
            }
            Thread[] childThreads = new Thread[32];
            while (true) {
                nbThreads = group.enumerate(childThreads, false);
                if (nbThreads == childThreads.length) {
                    childThreads = new Thread[childThreads.length * 2];
                } else {
                    break;
                }
            }
            threads = new ArrayList<ThreadData>();
            for (Thread t : childThreads) {
                if (t != null) {
                    threads.add(new ThreadData(t, infos.get(t.getId())));
                }
            }
        }

        public void print(String indent) {
            if (empty || hasThreads()) {
                if (!flat && !dump) {
                    System.out.println(indent + "Thread Group \"" + group.getName() + "\"");
                }
                for (ThreadGroupData tgd : groups) {
                    tgd.print(indent + (flat || dump ? "" : "    "));
                }
                for (ThreadData td : threads) {
                    td.print(indent + (flat ? "" : "    "));
                }
            }
        }

        public boolean hasThreads() {
            if (!threads.isEmpty()) {
                return true;
            }
            for (ThreadGroupData tgd : groups) {
                if (tgd.hasThreads()) {
                    return true;
                }
            }
            return false;
        }
    }

    public class ThreadData {
        private final Thread thread;
        private ThreadInfo info;

        public ThreadData(Thread thread, ThreadInfo info) {
            this.thread = thread;
            this.info = info;
        }

        public void print(String indent) {
            if (dump && info != null) {
                if (!prune || isInteresting()) {
                    printThreadInfo("    ");
                    //printLockInfo("    ");
                    //printMonitorInfo("    ");
                }
            } else {
                System.out.println(indent + (flat ? "" : "    ") + "\"" + thread.getName() + "\": " + thread.getState());
            }
        }

        private boolean isInteresting() {
            StackTraceElement[] stacktrace = info.getStackTrace();
            for (int i = 0; i < stacktrace.length; i++) {
                StackTraceElement ste = stacktrace[i];
                if (!ste.getClassName().startsWith("java.") && !ste.getClassName().startsWith("sun.")) {
                    return true;
                }
            }
            return false;
        }

        private void printThreadInfo(String indent) {
            // print thread information
            printThread(indent);

            // print stack trace with locks
            StackTraceElement[] stacktrace = info.getStackTrace();
            MonitorInfo[] monitors = info.getLockedMonitors();
            for (int i = 0; i < stacktrace.length; i++) {
                StackTraceElement ste = stacktrace[i];
                System.out.println(indent + "at " + ste.toString());
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == i) {
                        System.out.println(indent + "  - locked " + mi);
                    }
                }
            }
            System.out.println();
        }

        private void printThread(String indent) {
            StringBuilder sb = new StringBuilder("\"" + info.getThreadName() + "\"" + " Id="
                    + info.getThreadId() + " in " + info.getThreadState());
            if (info.getLockName() != null) {
                sb.append(" on lock=" + info.getLockName());
            }
            if (info.isSuspended()) {
                sb.append(" (suspended)");
            }
            if (info.isInNative()) {
                sb.append(" (running in native)");
            }
            System.out.println(sb.toString());
            if (info.getLockOwnerName() != null) {
                System.out.println(indent + " owned by " + info.getLockOwnerName() + " Id="
                        + info.getLockOwnerId());
            }
        }

        private void printMonitorInfo(String indent) {
            MonitorInfo[] monitors = info.getLockedMonitors();
            if (monitors != null && monitors.length > 0) {
                System.out.println(indent + "Locked monitors: count = " + monitors.length);
                for (MonitorInfo mi : monitors) {
                    System.out.println(indent + "  - " + mi + " locked at ");
                    System.out.println(indent + "      " + mi.getLockedStackDepth() + " "
                            + mi.getLockedStackFrame());
                }
                System.out.println();
            }
        }

        private void printLockInfo(String indent) {
            LockInfo[] locks = info.getLockedSynchronizers();
            if (locks != null && locks.length > 0) {
                System.out.println(indent + "Locked synchronizers: count = " + locks.length);
                for (LockInfo li : locks) {
                    System.out.println(indent + "  - " + li);
                }
                System.out.println();
            }
        }

    }

}
