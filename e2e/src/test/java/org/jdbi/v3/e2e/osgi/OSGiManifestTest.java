/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.e2e.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OSGiManifestTest {

    // Bundle-SymbolicName (= moduleName) of every published jdbi module; keep in sync with this module's test dependencies
    private static final List<String> EXPECTED_BUNDLES = List.of(
            "org.jdbi.v3.core",
            "org.jdbi.v3.sqlobject",
            "org.jdbi.v3.json",
            "org.jdbi.v3.jackson2",
            "org.jdbi.v3.jackson3",
            "org.jdbi.v3.postgres",
            "org.jdbi.v3.caffeine",
            "org.jdbi.v3.cache.noop",
            "org.jdbi.v3.commonstext",
            "org.jdbi.v3.freemarker",
            "org.jdbi.v3.generator",
            "org.jdbi.v3.gson2",
            "org.jdbi.v3.guava",
            "org.jdbi.v3.guice",
            "org.jdbi.v3.jodatime2",
            "org.jdbi.v3.jpa",
            "org.jdbi.v3.kotlin",
            "org.jdbi.v3.sqlobject.kotlin",
            "org.jdbi.v3.moshi",
            "org.jdbi.v3.mysql",
            "org.jdbi.v3.opentelemetry",
            "org.jdbi.v3.oracle12",
            "org.jdbi.v3.postgis",
            "org.jdbi.v3.spring",
            "org.jdbi.v3.spring5",
            "org.jdbi.v3.sqlite",
            "org.jdbi.v3.stringtemplate4",
            "org.jdbi.v3.testcontainers",
            "org.jdbi.v3.testing",
            "org.jdbi.v3.vavr");

    @Test
    void everyPublishedModuleHasAnOsgiBundleManifest() throws IOException {
        Set<String> bundleNames = new HashSet<>();
        Enumeration<URL> manifests = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            try (InputStream in = manifests.nextElement().openStream()) {
                String symbolicName = new Manifest(in).getMainAttributes().getValue("Bundle-SymbolicName");
                if (symbolicName != null) {
                    bundleNames.add(symbolicName);
                }
            }
        }

        assertThat(bundleNames).containsAll(EXPECTED_BUNDLES);
    }
}
