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
package org.apache.karaf.admin.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;


import junit.framework.TestCase;
import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.command.AdminCommandSupport;
import org.apache.karaf.admin.internal.AdminServiceImpl;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.junit.rules.TestName;

import static org.junit.Assert.*;

public class ExecuteTest {
    private String userDir;

    @Rule
    public TestName testName = new TestName();
    
    @Before
    public void setUp() throws Exception {
        Execute.exitAllowed = false;
        userDir = System.getProperty("user.dir");
    }

    @After
    public void tearDown() throws Exception {
        Execute.exitAllowed = true;
        System.setProperty("user.dir", userDir);
    }

    @Test
    public void testListCommands() throws Exception {
        PrintStream oldOut = System.out;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(baos); 
        System.setOut(capturedOut);

        try {
            Execute.main(new String [] {});            
        } catch (RuntimeException re) {
            assertEquals("0", re.getMessage());

            String s = new String(baos.toByteArray());            
            assertTrue(s.contains("list"));
            assertTrue(s.contains("create"));
            assertTrue(s.contains("destroy"));
        } finally {
            System.setOut(oldOut);
        }
    }

    @Test
    public void testNonexistingCommand() throws Exception {
        try {
            Execute.main(new String [] {"bheuaark"});
        } catch (RuntimeException re) {
            assertEquals("-1", re.getMessage());
        }
    }

    @Test
    public void testNoStorageFile() throws Exception {
        PrintStream oldErr = System.err;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capturedErr = new PrintStream(baos); 
        System.setErr(capturedErr);

        try {
            Execute.main(new String [] {"create"});            
        } catch (RuntimeException re) {
            assertEquals("-1", re.getMessage());
            
            String s = new String(baos.toByteArray());            
            assertTrue(s.contains("karaf.instances"));
            assertTrue(s.contains("instance.properties"));
        } finally {
            System.setErr(oldErr);
        } 
    }

    @Test
    public void testSetDir() throws Exception {
        Properties oldProps = (Properties) System.getProperties().clone();
        final File tempFile = createTempDir(testName.getMethodName());
        assertFalse("Precondition failed", 
            tempFile.getParentFile().getParentFile().getCanonicalPath().equals(System.getProperty("user.dir")));

        System.setProperty("karaf.instances", tempFile.getCanonicalPath());
        try {
            Execute.main(new String [] {"list"});            
            assertTrue(tempFile.getParentFile().getParentFile().getCanonicalPath().equals(System.getProperty("user.dir")));
        } finally {
            System.setProperties(oldProps);
            assertNull("Postcondition failed", System.getProperty("karaf.instances"));
            delete(tempFile);
        }        
    }

    @Ignore("http://fusesource.com/issues/browse/ENTESB-901")
    @Test
    public void testExecute() throws Exception {
        final File tempFile = createTempDir(testName.getMethodName());
        Properties p = new Properties();
        p.setProperty("ssh.port", "1302");
        p.setProperty("rmi.registry.port", "1122");
        p.setProperty("rmi.server.port", "44444");
        FileOutputStream fos = new FileOutputStream(new File(tempFile, AdminServiceImpl.STORAGE_FILE));
        p.store(fos, "");
        fos.close();

        final List<AdminServiceImpl> admins = new ArrayList<AdminServiceImpl>();
        try {
            AdminCommandSupport mockCommand = EasyMock.createStrictMock(AdminCommandSupport.class);
            mockCommand.setAdminService((AdminService) EasyMock.anyObject());
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    AdminServiceImpl svc = (AdminServiceImpl) EasyMock.getCurrentArguments()[0];
                    assertEquals(tempFile, svc.getStorageLocation());
                    admins.add(svc);
                    return null;
                }
            });
            
            EasyMock.expect(mockCommand.execute(null)).andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    // The Admin Service should be initialized at this point.
                    // One way to find this out is by reading out the port number
                    AdminServiceImpl admin = admins.get(0);
                    Field sshField = AdminServiceImpl.class.getDeclaredField("defaultSshPortStart");
                    sshField.setAccessible(true);
                    assertEquals(1302, sshField.get(admin));
                    Field rmiRegistryField = AdminServiceImpl.class.getDeclaredField("defaultRmiRegistryPortStart");
                    rmiRegistryField.setAccessible(true);
                    assertEquals(1122, rmiRegistryField.get(admin));
                    Field rmiServerField = AdminServiceImpl.class.getDeclaredField("defaultRmiServerPortStart");
                    rmiServerField.setAccessible(true);
                    assertEquals(44444, rmiServerField.get(admin));
                    return null;
                }
            });
            EasyMock.replay(mockCommand);            
            
            Execute.execute(mockCommand, tempFile, new String [] {"test"});
            
            EasyMock.verify(mockCommand);
        } finally {
            delete(tempFile);
        }
    }

    private static File createTempDir(String name) throws IOException {
        final File tempFile = File.createTempFile(name, null);
        tempFile.delete();
        tempFile.mkdirs();
        return tempFile.getCanonicalFile();
    }

    private static void delete(File tmp) {
        if (tmp.isDirectory()) {
            for (File f : tmp.listFiles()) {
                delete(f);
            }
        }
        tmp.delete();
    }
}
