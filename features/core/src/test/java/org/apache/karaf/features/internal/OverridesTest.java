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
package org.apache.karaf.features.internal;

import org.apache.felix.utils.manifest.Clause;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case for the {@link org.apache.karaf.features.internal.Overrides} class
 */
public class OverridesTest {

    @Test
    public void testLoadOverrides() {
        List<Clause> overrides = Overrides.loadOverrides("file:src/test/resources/etc/overrides.properties");
        assertEquals(2, overrides.size());

        Clause karafAdminCommand = null;
        Clause karafAdminCore = null;
        for (Clause clause : overrides) {
            if (clause.getName().equals("mvn:org.apache.karaf.admin/org.apache.karaf.admin.command/2.3.0.redhat-61033X")) {
                karafAdminCommand = clause;
            }
            if (clause.getName().equals("mvn:org.apache.karaf.admin/org.apache.karaf.admin.core/2.3.0.redhat-61033X")) {
                karafAdminCore = clause;
            }
        }
        assertNotNull("Missing admin.command bundle override", karafAdminCommand);
        assertNotNull("Missing admin.core bundle override", karafAdminCore);
        assertNotNull("Missing range on admin.core override", karafAdminCore.getAttribute(Overrides.OVERRIDE_RANGE));
    }

}
