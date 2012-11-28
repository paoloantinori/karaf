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

import java.util.List;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

@Command(scope = "jaas", name = "realms", description = "List JAAS Realms")
public class ListRealmsCommand extends JaasCommandSupport {

    private static final String REALM_LIST_FORMAT = "%5s %-20s %-80s";

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

    protected Object doExecute() throws Exception {
        List<JaasRealm> realms = getRealms();
        if (realms != null && realms.size() > 0) {
            System.out.println(String.format(REALM_LIST_FORMAT, "Index", "Realm", "Module Class"));
            Map<AppConfigurationEntry, JaasRealm> appConfigurationEntries = findEntries();
            if (!appConfigurationEntries.isEmpty()) {
                int index = 1;
                for (AppConfigurationEntry entry : appConfigurationEntries.keySet()) {
                    String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                    System.out.println(String.format(REALM_LIST_FORMAT, index++, appConfigurationEntries.get(entry).getName(), moduleClass));
                }
            }
        } else {
            System.err.println("No realm found");
        }
        return null;
    }

}
