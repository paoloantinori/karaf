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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Command(scope = "jaas", name = "manage", description = "Manage users and roles of a JAAS Realm")
public class ManageRealmCommand extends JaasCommandSupport {

    @Option(name = "--realm", description = "Realm Name", required = false, multiValued = false)
    String realmName;

    @Option(name = "--index", description = "Realm Index", required = false, multiValued = false)
    int index;

    @Option(name = "--module", aliases = {}, description = "Login Module Class Name", required = false, multiValued = false)
    String moduleName;

    @Option(name = "-f", aliases = {"--force"}, description = "Force the management of this realm, even if another one was under management", required = false, multiValued = false)
    boolean force;

    @Override
    protected Object doExecute() throws Exception {
        if (realmName == null && index <= 0) {
            System.err.println("A valid realm or the realm index need to be specified");
            return null;
        }
        JaasRealm oldRealm = (JaasRealm) this.session.get(JAAS_REALM);
        AppConfigurationEntry oldEntry = (AppConfigurationEntry) this.session.get(JAAS_ENTRY);

        if (oldRealm != null && !oldRealm.getName().equals(realmName) && !force) {
            System.err.println("Another JAAS Realm is being edited. Cancel/update first, or use the --force option.");
        } else if (oldEntry != null && !oldEntry.getLoginModuleName().equals(moduleName) && !force) {
            System.err.println("Another JAAS Login Module is being edited. Cancel/update first, or use the --force option.");
        } else {
            Map<AppConfigurationEntry, JaasRealm> entries = findEntries(realmName, moduleName, index);
            if (entries.size() == 1) {
                Queue<JaasCommandSupport> commands = null;

                commands = (Queue<JaasCommandSupport>) this.session.get(JAAS_CMDS);
                if (commands == null) {
                    commands = new LinkedList<JaasCommandSupport>();
                }
                AppConfigurationEntry entry = entries.keySet().iterator().next();
                JaasRealm realm = entries.get(entry);
                this.session.put(JAAS_REALM, realm);
                this.session.put(JAAS_ENTRY, entry);
                this.session.put(JAAS_CMDS, commands);
            } else {
                reportFailure(realmName, moduleName, index);
            }
        }
        return null;
    }

    private void reportFailure(String realmName, String moduleName, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to find Login Module with");
        boolean isAndRequired = false;
        if (realmName != null && !realmName.isEmpty()) {
            sb.append(" realm name:").append(realmName);
            isAndRequired = true;
        }
        if (index > 0) {
            if (isAndRequired) {
                sb.append(" and");
            }
            sb.append(" index:").append(index);
            isAndRequired = true;
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            if (isAndRequired) {
                sb.append(" and");
            }
            sb.append(" module name:").append(moduleName);
        }

        if (realmName != null && findRealmsByName(realmName).size() > 0 && index == 0 && moduleName == null) {
            sb.append(".\n");
            sb.append("Reason: Multiple realms named ").append(realmName).append(" found.Please specify the index or the module name of the target realm. ");
        } else {
            sb.append(".");
        }
        System.err.println(sb.toString());
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}
