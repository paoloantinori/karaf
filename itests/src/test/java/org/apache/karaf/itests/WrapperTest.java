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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.File;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class WrapperTest extends KarafTestSupport {

    @Before
    public void installObrFeature() throws Exception {
        System.out.println(executeCommand("features:install wrapper", ADMIN_ROLE));
        // give it time on faster machines to complete
        Thread.sleep(500);
    }


    @Test
    public void installCommand() throws Exception {
        String installOutput = executeCommand("wrapper:install", ADMIN_ROLE);
        System.out.println(installOutput);
        assertFalse(installOutput.isEmpty());

        assertTrue("wrapper:install should print path to karaf-wrapper",
                countMatches(".*karaf-wrapper.conf", installOutput) > 0);

        if (!System.getProperty("os.name").startsWith("Windows")) {
            assertTrue("wrapper:install should print what to 'symlink'",
                    countMatches(".*ln -s.*", installOutput) > 0);
        }

        String karafHome = System.getProperty("karaf.home");
        File karafService = new File(karafHome + File.separator + "bin", "karaf-service");
        assertTrue(karafService + " script should exist", karafService.exists());
    }
}
