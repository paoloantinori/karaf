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
package org.apache.karaf.features.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Overrides {

    private static final Logger LOGGER = LoggerFactory.getLogger(Overrides.class);

    private static final String OVERRIDE_RANGE = ";range=";

    private static final Pattern SYMBOLIC_NAME_PATTERN = Pattern.compile("([^;: ]+)(.*)");

    /*
    public static Map<Long, String> override(BundleContext context, String overridesUrl) {
        List<String> overrides = loadOverrides(overridesUrl);
        if (overrides.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Manifest> manifests = new HashMap<String, Manifest>();
            for (String override : overrides) {
                Manifest manifest = getManifest(override);
                manifests.put(override, manifest);
            }
            Map<Long, String> map = new HashMap<Long, String>();

            Bundle bundle = context.getBundles();

        } catch (Exception e) {
            LOGGER.info("Unable to process bundle overrides", e);
            return Collections.emptyMap();
        }
    }
    */

    public static List<BundleInfo> override(List<BundleInfo> infos, String overridesUrl) {
        List<String> overrides = loadOverrides(overridesUrl);
        if (overrides.isEmpty()) {
            return infos;
        }
        try {
            Map<String, Manifest> manifests = new HashMap<String, Manifest>();
            for (String override : overrides) {
                Manifest manifest = getManifest(override);
                manifests.put(override, manifest);
            }
            List<BundleInfo> newInfos = new ArrayList<BundleInfo>();
            for (BundleInfo info : infos) {
                Manifest manifest = getManifest(info.getLocation());
                if (manifest != null) {
                    String bsn = getBundleSymbolicName(manifest);
                    Version ver = getBundleVersion(manifest);
                    String url = info.getLocation();
                    for (String override : overrides) {
                        Manifest overMan = manifests.get(override);
                        if (overMan == null) {
                            continue;
                        }
                        String oBsn = getBundleSymbolicName(overMan);
                        if (!bsn.equals(oBsn)) {
                            continue;
                        }

                        VersionRange range;
                        String vr = extractVersionRange(override);
                        if (vr == null) {
                            // default to micro version compatibility
                            Version v2 = new Version(ver.getMajor(), ver.getMinor() + 1, 0);
                            range = new VersionRange(false, ver, v2, true);
                        } else {
                            range = VersionRange.parseVersionRange(vr);
                        }

                        Version oVer = getBundleVersion(overMan);

                        // The resource matches, so replace it with the overridden resource
                        // if the override is actually a newer version than what we currently have
                        if (range.contains(ver) && ver.compareTo(oVer) < 0) {
                            ver = oVer;
                            url = override;
                        }
                    }
                    if (!info.getLocation().equals(url)) {
                        newInfos.add(new BundleInfoImpl(url, info.getStartLevel(), info.isStart(), info.isDependency()));
                    } else {
                        newInfos.add(info);
                    }
                } else {
                    newInfos.add(info);
                }
            }
            return newInfos;
        } catch (Exception e) {
            LOGGER.info("Unable to process bundle overrides", e);
            return infos;
        }
    }

    public static List<String> loadOverrides(String overridesUrl) {
        List<String> overrides = new ArrayList<String>();
        try {
            if (overridesUrl != null) {
                InputStream is = new URL(overridesUrl).openStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            overrides.add(line);
                        }
                    }
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to load overrides bundles list", e);
        }
        return overrides;
    }

    private static Version getBundleVersion(Manifest manifest) {
        String ver = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        return VersionTable.getVersion(ver);
    }

    private static String getBundleSymbolicName(Manifest manifest) {
        String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        bsn = stripSymbolicName(bsn);
        return bsn;
    }

    private static Manifest getManifest(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry = null;
            while ( (entry = zis.getNextEntry()) != null ) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    return new Manifest(zis);
                }
            }
            return null;
        } finally {
            is.close();
        }
    }

    private static String extractVersionRange(String override) {
        return override.contains(OVERRIDE_RANGE) ? override.split(OVERRIDE_RANGE)[1] : null;
    }

    private static String stripSymbolicName(String symbolicName) {
        Matcher m = SYMBOLIC_NAME_PATTERN.matcher(symbolicName);
        if (m.matches() && m.groupCount() >= 1) {
            return m.group(1);
        } else {
            return symbolicName;
        }
    }
}
