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
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.PackageAdmin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Command(scope = "dev", name = "load-test", description = "Load test bundle lifecycle")
public class LoadTest extends OsgiCommandSupport {

    @Option(name = "--threads", description = "number of concurrent threads")
    int threads = 2;

    @Option(name = "--delay", description = "maximum delay between actions")
    int delay = 1;

    @Option(name = "--iterations", description = "number of iterations per thread")
    int iterations = 100;

    @Option(name = "--refresh", description = "percentage of bundle refresh vs restart")
    int refresh = 20;

    @Option(name = "--excludes", description = "List of bundles (ids or symbolic names) to exclude")
    List<String> excludes = Arrays.asList("0", "org.ops4j.pax.url.mvn", "org.ops4j.pax.logging.pax-logging-api", "org.ops4j.pax.logging.pax-logging-service");

    @Override
    protected Object doExecute() throws Exception {
        if (!confirm(session)) {
            return null;
        }
        final BundleContext bundleContext = getBundleContext().getBundle(0).getBundleContext();
        final FrameworkWiring wiring = bundleContext.getBundle().adapt(FrameworkWiring.class);
        final CountDownLatch latch = new CountDownLatch(threads);
        final Bundle[] bundles = bundleContext.getBundles();
        final AtomicBoolean[] locks = new AtomicBoolean[bundles.length];
        for (int b = 0; b < locks.length; b++) {
            locks[b] = new AtomicBoolean(true);
            // Avoid touching excluded bundles
            if (excludes.contains(Long.toString(bundles[b].getBundleId()))
                    || excludes.contains(bundles[b].getSymbolicName())) {
                continue;
            }
            // Only touch active bundles
            if (bundles[b].getState() != Bundle.ACTIVE) {
                continue;
            }
            // Now set the lock to available
            locks[b].set(false);
        }
        for (int i = 0; i < threads; i++) {
            new Thread() {
                public void run() {
                    try {
                        Random rand = new Random();
                        for (int j = 0; j < iterations; j++) {
                            for (;;) {
                                int b = rand.nextInt(bundles.length);
                                if (locks[b].compareAndSet(false, true)) {
                                    try {
                                        // Only touch active bundles
                                        if (bundles[b].getState() != Bundle.ACTIVE) {
                                            continue;
                                        }
                                        if (rand.nextInt(100) < refresh) {
                                            bundles[b].update();
                                            final CountDownLatch latch = new CountDownLatch(1);
                                            wiring.refreshBundles(Collections.singletonList(bundles[b]), new FrameworkListener() {
                                                public void frameworkEvent(FrameworkEvent event) {
                                                    latch.countDown();
                                                }
                                            });
                                            latch.await();
                                        } else {
                                            for (int i = 5; i >= 0; i--) {
                                                try {
                                                    bundles[b].stop(Bundle.STOP_TRANSIENT);
                                                    break;
                                                } catch (Exception e) {
                                                    if (i > 0) {
                                                        Thread.sleep(1);
                                                    } else {
                                                        throw e;
                                                    }
                                                }
                                            }
                                            for (int i = 100; i >= 0; i--) {
                                                try {
                                                    bundles[b].start(Bundle.START_TRANSIENT);
                                                    break;
                                                } catch (Exception e) {
                                                    if (i > 0) {
                                                        Thread.sleep(1);
                                                    } else {
                                                        throw e;
                                                    }
                                                }
                                            }
                                        }
                                        Thread.sleep(rand.nextInt(delay));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        locks[b].set(false);
                                    }
                                }
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }.start();
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("Load test finished");
            }
        }.start();
        return null;
    }

    private boolean confirm(CommandSession session) throws IOException {
        for (;;) {
            StringBuffer sb = new StringBuffer();
            System.err.print("You are about to perform a start/stop/refresh load test on bundles.\nDo you wish to continue (yes/no): ");
            System.err.flush();
            for (;;) {
                int c = session.getKeyboard().read();
                if (c < 0) {
                    return false;
                }
                if (c == '\r' || c == '\n') {
                    System.err.println();
                    System.err.flush();
                    break;
                }
                System.err.print((char) c);
                System.err.flush();
                sb.append((char) c);
            }
            String str = sb.toString();
            if ("yes".equals(str)) {
                return true;
            }
            if ("no".equals(str)) {
                return false;
            }
        }
    }
}
