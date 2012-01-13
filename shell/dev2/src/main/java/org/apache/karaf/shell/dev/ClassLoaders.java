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

import java.util.Collection;

import org.apache.felix.framework.monitor.MonitoringService;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

@Command(scope = "dev", name = "classloaders", description = "Show leaking bundle classloaders.")
public class ClassLoaders extends OsgiCommandSupport {

    private MonitoringService monitoringService;

    @Override
    protected Object doExecute() throws Exception {
        Collection<ClassLoader> cls = monitoringService.getClassLoaders();
        boolean leaking = false;
        for (ClassLoader cl : cls) {
            Bundle bundle = ((BundleReference) cl).getBundle();
            leaking |= (bundle.getState() < Bundle.RESOLVED);
        }
        if (leaking) {
            System.out.println("Leaking classloaders for bundles:");
            for (ClassLoader cl : cls) {
                Bundle bundle = ((BundleReference) cl).getBundle();
                if (bundle.getState() < Bundle.RESOLVED) {
                    System.out.println("  " + bundle.getSymbolicName() + " " + bundle.getVersion());
                }
            }
        } else {
            System.out.println("No classloader leaks detected for bundles");
        }
        return null;
    }

    public MonitoringService getMonitoringService() {
        return monitoringService;
    }

    public void setMonitoringService(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }
}
