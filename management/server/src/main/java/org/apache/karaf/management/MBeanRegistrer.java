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
package org.apache.karaf.management;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class MBeanRegistrer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MBeanRegistrer.class);

    private final ConcurrentMap<MBeanServer, Set<String>> mbeanServers = new ConcurrentHashMap<MBeanServer, Set<String>>();
    private Map<Object, String> mbeans;
    private BundleContext context;
    private ServiceTracker tracker;


    public void setMbeans(Map<Object, String> mbeans) {
        this.mbeans = mbeans;
    }

    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    public void init() throws Exception {
        tracker = new ServiceTracker(context, MBeanServer.class.getName(), new ServiceTrackerCustomizer() {
            public Object addingService(ServiceReference reference) {
                Object service = context.getService(reference);
                registerMBeans((MBeanServer) service);
                return service;
            }
            public void modifiedService(ServiceReference reference, Object service) {
            }
            public void removedService(ServiceReference reference, Object service) {
                unregisterMBeans((MBeanServer) service);
                context.ungetService(reference);
            }
        });
        tracker.open();
    }

    public void destroy() throws Exception {
        tracker.close();
    }

    protected void registerMBeans(MBeanServer mbeanServer) {
        if (mbeanServer != null && mbeans != null) {
            Set<String> registered = new HashSet<String>();
            if (mbeanServers.putIfAbsent(mbeanServer, registered) == null) {
                for (Map.Entry<Object, String> entry : mbeans.entrySet()) {
                    String name = parseProperty(entry.getValue());
                    try {
                        ObjectName oname = new ObjectName(name);
                        try {
                            mbeanServer.registerMBean(entry.getKey(), oname);
                        } catch (InstanceAlreadyExistsException e) {
                            // If the mbean is already registered, unregister and try again
                            mbeanServer.unregisterMBean(oname);
                            mbeanServer.registerMBean(entry.getKey(), oname);
                        }
                        registered.add(name);
                    } catch (JMException e) {
                        LOGGER.warn("Unable to register mbean {}", name, e);
                    }
                }
            }
        }
    }

    protected void unregisterMBeans(MBeanServer mbeanServer) {
        if (mbeanServer != null && mbeans != null) {
            Set<String> registered = mbeanServers.remove(mbeanServer);
            if (registered != null) {
                while (!registered.isEmpty()) {
                    String name = registered.iterator().next();
                    try {
                        mbeanServer.unregisterMBean(new ObjectName(name));
                    } catch (JMException e) {
                        LOGGER.warn("Unable to unregister mbean {}", name, e);
                    }
                    registered.remove(name);
                }
            }
        }
    }

    protected String parseProperty(String raw) {
        if (raw.indexOf("${") > -1 && raw.indexOf("}", raw.indexOf("${")) > -1) {
            String var = raw.substring(raw.indexOf("${") + 2, raw.indexOf("}"));
            String val = System.getProperty(var);
            if (val != null) {
                raw = raw.replace("${" + var + "}", val);
            }
        }
        return raw;
    }
}
