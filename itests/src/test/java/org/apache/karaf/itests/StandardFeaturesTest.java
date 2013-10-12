/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class StandardFeaturesTest extends KarafTestSupport {

    @Test
    public void testBootFeatures() throws Exception {
        // config
        String configFeatureStatus = executeCommand("features:list -i | grep config");
        System.out.println(configFeatureStatus);
        assertFalse("config feature is not installed", configFeatureStatus.isEmpty());
        // ssh
        String sshFeatureStatus = executeCommand("features:list -i | grep ssh");
        System.out.println(sshFeatureStatus);
        assertFalse("ssh feature is not installed", sshFeatureStatus.isEmpty());
        // management
        String managementFeatureStatus = executeCommand("features:list -i | grep management");
        System.out.println(managementFeatureStatus);
        assertFalse("management feature is not installed", managementFeatureStatus.isEmpty());
        // kar
        String karFeatureStatus = executeCommand("features:list -i | grep \"kar \"");
        System.out.println(karFeatureStatus);
        assertFalse("kar feature is not installed", karFeatureStatus.isEmpty());
    }


    @Test
    public void testWrapperFeature() throws Exception {
        System.out.println(executeCommand("features:install wrapper"));
        String wrapperFeatureStatus = executeCommand("features:list -i | grep wrapper");
        System.out.println(wrapperFeatureStatus);
        assertFalse("wrapper feature is not installed", wrapperFeatureStatus.isEmpty());
    }

    @Test
    public void testObrFeature() throws Exception {
        System.out.println(executeCommand("features:install obr"));
        String obrFeatureStatus = executeCommand("features:list -i | grep obr");
        System.out.println(obrFeatureStatus);
        assertFalse("obr feature is not installed", obrFeatureStatus.isEmpty());
    }

    @Test
    public void testJettyFeature() throws Exception {
        System.out.println(executeCommand("features:install jetty"));
        String jettyFeatureStatus = executeCommand("features:list -i | grep jetty");
        System.out.println(jettyFeatureStatus);
        assertFalse("jetty feature is not installed", jettyFeatureStatus.isEmpty());
    }

    @Test
    public void testHttpFeature() throws Exception {
        System.out.println(executeCommand("features:install http"));
        String httpFeatureStatus = executeCommand("features:list -i | grep http");
        System.out.println(httpFeatureStatus);
        assertFalse("http feature is not installed", httpFeatureStatus.isEmpty());
    }

    @Test
    public void testHttpWhiteboardFeature() throws Exception {
        System.out.println(executeCommand("features:install http-whiteboard"));
        String httpWhiteboardFeatureStatus = executeCommand("features:list -i | grep http-whiteboard");
        System.out.println(httpWhiteboardFeatureStatus);
        assertFalse("http-whiteboard feature is not installed", httpWhiteboardFeatureStatus.isEmpty());
    }

    @Test
    public void testWarFeature() throws Exception {
        System.out.println(executeCommand("features:install war"));
        String warFeatureStatus = executeCommand("features:list -i | grep war");
        System.out.println(warFeatureStatus);
        assertFalse("war feature is not installed", warFeatureStatus.isEmpty());
    }

    @Test
    public void testWebconsoleFeature() throws Exception {
        System.out.println(executeCommand("features:install webconsole"));
        String webconsoleFeatureStatus = executeCommand("features:list -i | grep webconsole");
        System.out.println(webconsoleFeatureStatus);
        assertFalse("webconsole feature is not installed", webconsoleFeatureStatus.isEmpty());
    }

    @Test
    public void testEventadminFeature() throws Exception {
        System.out.println(executeCommand("features:install eventadmin"));
        String eventadminFeatureStatus = executeCommand("features:list -i | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertFalse("eventadmin feature is not installed", eventadminFeatureStatus.isEmpty());
    }

    @Test
    public void testJasyptEncryptionFeature() throws Exception {
        System.out.println(executeCommand("features:install jasypt-encryption"));
        String jasyptEncryptionFeatureStatus = executeCommand("features:list -i | grep jasypt-encryption");
        System.out.println(jasyptEncryptionFeatureStatus);
        assertFalse("jasypt-encryption feature is not installed", jasyptEncryptionFeatureStatus.isEmpty());
    }

}
