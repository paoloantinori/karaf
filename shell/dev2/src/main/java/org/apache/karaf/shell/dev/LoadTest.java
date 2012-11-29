package org.apache.karaf.shell.dev;

import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class LoadTest extends OsgiCommandSupport {

    @Option(name = "--threads", description = "number of concurrent threads")
    int threads = 2;

    @Option(name = "--delay", description = "maximum delay between actions")
    int delay = 1;

    @Option(name = "--iterations", description = "number of iterations per thread")
    int iterations = 100;

    @Option(name = "--refresh", description = "percentage of bundle refresh vs restart")
    int refresh = 20;

    @Override
    protected Object doExecute() throws Exception {
        final CountDownLatch latch = new CountDownLatch(threads);
        final Bundle[] bundles = getBundleContext().getBundles();
        final ServiceReference ref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        final PackageAdmin pa = (PackageAdmin) getBundleContext().getService(ref);
        for (int i = 0; i < threads; i++) {
            new Thread() {
                public void run() {
                    try {
                        Random rand = new Random();
                        for (int j = 0; j < iterations; j++) {
                            for (;;) {
                                int b = rand.nextInt(bundles.length);
                                if (bundles[b].getBundleId() == 0) {
                                    continue;
                                }
                                if (bundles[b].getState() != Bundle.ACTIVE) {
                                    continue;
                                }
                                if (rand.nextInt(100) < refresh) {
                                    bundles[b].update();
                                    pa.refreshPackages(null);
                                } else {
                                    bundles[b].stop(Bundle.STOP_TRANSIENT);
                                    bundles[b].start(Bundle.START_TRANSIENT);
                                }
                                Thread.sleep(rand.nextInt(delay));
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
                getBundleContext().ungetService(ref);
                System.err.println("Load test finished");
            }
        }.start();
        return null;
    }

}
