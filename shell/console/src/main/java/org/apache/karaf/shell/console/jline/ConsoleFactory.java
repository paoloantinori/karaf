/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console.jline;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyPair;
import java.nio.charset.Charset;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.Subject;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.karaf.jaas.modules.JaasHelper;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleFactory {

    private static final Class[] SECURITY_BUGFIX = {
            JaasHelper.class,
            JaasHelper.OsgiSubjectDomainCombiner.class,
            JaasHelper.DelegatingProtectionDomain.class,
    };

    private BundleContext bundleContext;
    private TerminalFactory terminalFactory;
    Console console;
    private boolean start;
    private ServiceRegistration registration;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void registerCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        stop();
        start(commandProcessor);
    }

    public void unregisterCommandProcessor(CommandProcessor commandProcessor) throws Exception {
        stop();
    }

    public void setTerminalFactory(TerminalFactory terminalFactory) {
        this.terminalFactory = terminalFactory;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    protected void start(final CommandProcessor commandProcessor) throws Exception {
        if (start) {
            Subject subject = new Subject();
            final String user = "karaf";
            subject.getPrincipals().add(new UserPrincipal(user));
            JaasHelper.doAs(subject, new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    doStart(user, commandProcessor);
                    return null;
                }
            });
        }
    }

    public static Object invokePrivateMethod(Object o, String methodName, Object[] params) throws Exception {
        final Method methods[] = o.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methodName.equals(methods[i].getName())) {
                methods[i].setAccessible(true);
                return methods[i].invoke(o, params);
            }
        }
        return null;
    }
    
    private static <T> T unwrapBIS(T stream) {
        try {
             return (T) invokePrivateMethod(stream, "getInIfOpen", null);
        } catch (Throwable t) {
             return stream;
        }
    }

    protected void doStart(String user, CommandProcessor commandProcessor) throws Exception {
        final Terminal terminal = terminalFactory.getTerminal();
        // unwrap stream so it can be recognized by the terminal and wrapped to get 
        // special keys in windows
        InputStream unwrappedIn = unwrapBIS(unwrap(System.in));
        InputStream in = terminal.wrapInIfNeeded(unwrappedIn);
        PrintStream out = unwrap(System.out);
        PrintStream err = unwrap(System.err);
        Runnable callback = new Runnable() {
            public void run() {
                try {
                    bundleContext.getBundle(0).stop();
                } catch (Exception e) {
                    // Ignore
                }
            }
        };
        String ctype = System.getenv("LC_CTYPE");
        String encoding = ctype;
        if (encoding != null && encoding.indexOf('.') > 0) {
            encoding = encoding.substring(encoding.indexOf('.') + 1);
        } else {
            encoding = System.getProperty("input.encoding", Charset.defaultCharset().name());
        }
        Console console = new Console(commandProcessor,
                                   in,
                                   wrap(out),
                                   wrap(err),
                                   terminal,
                                   encoding,
                                   callback);
        CommandSession session = console.getSession();
        for (Object o : System.getProperties().keySet()) {
            String key = o.toString();
            session.put(key, System.getProperty(key));
        }
        session.put("USER", user);
        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
        session.put("#LINES", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getHeight());
            }
        });
        session.put("#COLUMNS", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getWidth());
            }
        });
        if (ctype != null) {
            session.put("LC_CTYPE", ctype);
        }
        session.put(".jline.terminal", terminal);

        ServiceRegistration registration = bundleContext.registerService(CommandSession.class, session, null);

        boolean used = false;
        synchronized (this) {
            if (this.console == null) {
                this.console = console;
                this.registration = registration;
                used = true;
            }
        }
        if (used) {
            boolean delayconsole = Boolean.parseBoolean(System.getProperty("karaf.delay.console"));
            if (delayconsole) {
                new DelayedStarted(console, bundleContext, unwrappedIn).start();
            } else {
                new Thread(console, "Karaf Shell Console Thread").start();
            }
        } else {
            registration.unregister();
            console.close(false);
        }
    }

    protected void stop() throws Exception {
        ServiceRegistration reg;
        Console cns;
        synchronized (this) {
            reg = registration;
            registration = null;
            cns = console;
            console = null;
        }
        if (reg != null) {
            reg.unregister();
        }
        // The bundle is stopped
        // so close the console and remove the callback so that the
        // osgi framework isn't stopped
        if (cns != null) {
            cns.close(false);
        }
    }

    private static PrintStream wrap(PrintStream stream) {
        OutputStream o = AnsiConsole.wrapOutputStream(stream);
        if (o instanceof PrintStream) {
            return ((PrintStream) o);
        } else {
            return new PrintStream(o);
        }
    }

    private static <T> T unwrap(T stream) {
        try {
            Method mth = stream.getClass().getMethod("getRoot");
            return (T) mth.invoke(stream);
        } catch (Throwable t) {
            return stream;
        }
    }
}
