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
package org.apache.karaf.jaas.modules.properties;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PropertiesBackingEngine implements BackingEngine {

    private final Logger logger = LoggerFactory.getLogger(PropertiesBackingEngine.class);

    private Properties users;
    private EncryptionSupport encryptionSupport;

    /**
     * Constructor
     *
     * @param users
     */
    public PropertiesBackingEngine(Properties users) {
        this.users = users;
    }

    public PropertiesBackingEngine(Properties users, EncryptionSupport encryptionSupport) {
        this.users = users;
        this.encryptionSupport = encryptionSupport;
    }

    /**
     * Add a user.
     *
     * @param username
     * @param password
     */
    public void addUser(String username, String password) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String newPassword = password;

        //If encryption support is enabled, encrypt password
        if (encryptionSupport != null && encryptionSupport.getEncryption() != null) {
            newPassword = encryptionSupport.getEncryption().encryptPassword(password);
            if (encryptionSupport.getEncryptionPrefix() != null) {
                newPassword = encryptionSupport.getEncryptionPrefix() + newPassword;
            }
            if (encryptionSupport.getEncryptionSuffix() != null) {
                newPassword = newPassword + encryptionSupport.getEncryptionSuffix();
            }
        }

        String userInfos = users.get(username);

        //If user already exists, update password
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            userInfoBuffer.append(newPassword);

            for (int i = 1; i < infos.length; i++) {
                userInfoBuffer.append(",");
                userInfoBuffer.append(infos[i]);
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        } else {
            users.put(username, newPassword);
        }

        try {
            users.save();
        } catch (Exception ex) {
            logger.error("Cannot update users file,", ex);
        }
    }

    /**
     * Delete a User.
     *
     * @param username
     */
    public void deleteUser(String username) {
        users.remove(username);

        try {
            users.save();
        } catch (Exception ex) {
            logger.error("Cannot remove users file,", ex);
        }
    }

    /**
     * List Users
     *
     * @return
     */
    public List<UserPrincipal> listUsers() {
        List<UserPrincipal> result = new ArrayList<UserPrincipal>();

        for (String userNames : users.keySet()) {
            UserPrincipal userPrincipal = new UserPrincipal(userNames);
            result.add(userPrincipal);
        }
        return result;
    }

    /**
     * List the Roles of the {@param user}
     *
     * @param user
     * @return
     */
    public List<RolePrincipal> listRoles(UserPrincipal user) {
        return listRoles(user.getName());
    }

    private List<RolePrincipal> listRoles(String name) {
        List<RolePrincipal> result = new ArrayList<RolePrincipal>();
        String userInfo = (String) users.get(name);
        String[] infos = userInfo.split(",");
        for (int i = 1; i < infos.length; i++) {
            result.add(new RolePrincipal(infos[i]));
        }
        return result;
    }

    /**
     * Add a role to a User.
     *
     * @param username
     * @param role
     */
    public void addRole(String username, String role) {
        String userInfos = users.get(username);
        if (userInfos != null) {
            for (RolePrincipal rp : listRoles(username)) {
                if (role.equals(rp.getName())) {
                    return; 
                }
            }
            String newUserInfos = userInfos + "," + role;
            users.put(username, newUserInfos);
        }
        try {
            users.save();
        } catch (Exception ex) {
            logger.error("Cannot update users file,", ex);
        }
    }

    /**
     * Delete a Role form User.
     *
     * @param username
     * @param role
     */
    public void deleteRole(String username, String role) {
        String[] infos = null;
        StringBuffer userInfoBuffer = new StringBuffer();

        String userInfos = users.get(username);

        //If user already exists, remove the role
        if (userInfos != null && userInfos.length() > 0) {
            infos = userInfos.split(",");
            String password = infos[0];
            userInfoBuffer.append(password);

            for (int i = 1; i < infos.length; i++) {
                if (infos[i] != null && !infos[i].equals(role)) {
                    userInfoBuffer.append(",");
                    userInfoBuffer.append(infos[i]);
                }
            }
            String newUserInfo = userInfoBuffer.toString();
            users.put(username, newUserInfo);
        }

        try {
            users.save();
        } catch (Exception ex) {
            logger.error("Cannot update users file,", ex);
        }
    }
}
