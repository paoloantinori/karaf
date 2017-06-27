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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CdiFeaturesTest extends KarafTestSupport {

    @Test
    public void testPaxCdiFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING pax-cdi FEATURE =====");
        installAndAssertFeature("pax-cdi", "1.0.0.RC1-redhat-001");
    }

    @Test
    public void testPaxCdiWebFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING pax-cdi-web FEATURE =====");
        installAndAssertFeature("pax-cdi-web", "1.0.0.RC1-redhat-001");
    }

    @Test
    public void testPaxCdiWeldFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING pax-cdi-weld FEATURE =====");
        installAndAssertFeature("pax-cdi-weld", "1.0.0.RC1-redhat-001");
    }

    @Test
    public void testPaxCdiWebWeldFeature() throws Exception {
        System.out.println("");
        System.out.println("===== TESTING pax-cdi-web-weld FEATURE =====");
        installAndAssertFeature("pax-cdi-web-weld", "1.0.0.RC1-redhat-001");
    }

}
