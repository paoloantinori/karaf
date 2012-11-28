/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.command;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineService;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class JaasCommandSupport extends OsgiCommandSupport {

    public static final String JAAS_REALM = "JaasCommand.REALM";
    public static final String JAAS_ENTRY = "JaasCommand.ENTRY";
    public static final String JAAS_CMDS = "JaasCommand.COMMANDS";

    private List<JaasRealm> realms;

    protected BackingEngineService backingEngineService;

    protected abstract Object doExecute(BackingEngine engine) throws Exception;

    /**
     * Add the command to the command queue.
     *
     * @return
     * @throws Exception
     */
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);
        Queue commandQueue = (Queue) session.get(JAAS_CMDS);

        if (realm != null && entry != null) {
            if (commandQueue != null) {
                commandQueue.add(this);
            }
        } else {
            System.err.println("No JAAS Realm / Module has been selected.");
        }
        return null;
    }


    /**
     * Lists Realms by name.
     * @param realmName
     * @return
     */
    public List<JaasRealm> findRealmsByName(String realmName) {
        List<JaasRealm> realmList = new LinkedList<JaasRealm>();
        for (JaasRealm realm : realms) {
            if (realm.getName().equals(realmName)) {
                realmList.add(realm);
            }
        }
        return realmList;
    }

    /**
     * Returns matching {@link AppConfigurationEntry} mapped to the matching {@link JaasRealm}.
     * @param realmName
     * @param moduleName
     * @param index
     * @return
     */
    public Map<AppConfigurationEntry, JaasRealm> findEntries(String realmName, String moduleName, int index) {
        Map<AppConfigurationEntry, JaasRealm> entries = new LinkedHashMap<AppConfigurationEntry, JaasRealm>();
        Map<AppConfigurationEntry, JaasRealm> appConfigurationEntries = findEntries();
        int i = 1;
        for (AppConfigurationEntry entry : appConfigurationEntries.keySet()) {
            JaasRealm realm = appConfigurationEntries.get(entry);
            String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
            if (realmName != null && !realmName.equals(realm.getName())) {
                i++;
                continue;
            } else if (moduleName != null && !moduleName.equals(entry.getLoginModuleName()) && !moduleName.equals(moduleClass)) {
                i++;
                continue;
            } else if (index != 0 && index != i) {
                i++;
                continue;
            } else {
                i++;
                entries.put(entry, realm);
            }
        }
        return entries;
    }

    /**
     * Returns all {@link AppConfigurationEntry} mapped to the matching {@link JaasRealm}.
     * @return
     */
    public Map<AppConfigurationEntry, JaasRealm> findEntries() {
        Map<AppConfigurationEntry, JaasRealm> appConfigurationEntries = new LinkedHashMap<AppConfigurationEntry, JaasRealm>();
        for (JaasRealm realm : realms) {
            AppConfigurationEntry[] configurationEntries = realm.getEntries();
            if (configurationEntries != null) {
                for (AppConfigurationEntry configurationEntry : configurationEntries) {
                    appConfigurationEntries.put(configurationEntry, realm);
                }
            }
        }
        return appConfigurationEntries;
    }


    public List<JaasRealm> getRealms() {
        return realms;
    }

    public void setRealms(List<JaasRealm> realms) {
        this.realms = realms;
    }

    public BackingEngineService getBackingEngineService() {
        return backingEngineService;
    }

    public void setBackingEngineService(BackingEngineService backingEngineService) {
        this.backingEngineService = backingEngineService;
    }
}
