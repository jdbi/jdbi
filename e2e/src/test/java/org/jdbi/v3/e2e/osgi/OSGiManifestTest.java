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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

class OSGiManifestTest {

    private record Bundle(String symbolicName, Attributes attributes) {
        @Override
        public String toString() {
            return symbolicName;
        }
    }

    @Test
    void everyDeclaredModuleProducedABundle() throws Exception {
        assertThat(jdbiBundlesOnClasspath()).hasSize(declaredJdbiTestDependencyCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jdbiBundlesOnClasspath")
    void bundleHasBasicInformation(Bundle bundle) {
        assertThat(bundle.attributes().getValue("Bundle-Name")).isNotBlank();
        assertThat(bundle.attributes().getValue("Bundle-Version")).isNotBlank();
    }

    @Test
    void allBundlesShareTheSameVersion() throws IOException {
        List<Bundle> bundles = jdbiBundlesOnClasspath();
        assertThat(bundles.stream().map(b -> b.attributes().getValue("Bundle-Version")))
                .containsOnly(bundles.get(0).attributes().getValue("Bundle-Version"));
    }

    // discovered from the classpath, not a fixed list: a module dropping its manifest fails
    // everyDeclaredModuleProducedABundle() instead of silently shrinking the coverage below
    static List<Bundle> jdbiBundlesOnClasspath() throws IOException {
        List<Bundle> bundles = new ArrayList<>();
        Enumeration<URL> manifests = OSGiManifestTest.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            try (InputStream in = manifests.nextElement().openStream()) {
                Attributes attributes = new Manifest(in).getMainAttributes();
                String symbolicName = attributes.getValue("Bundle-SymbolicName");
                if (symbolicName != null && symbolicName.startsWith("org.jdbi.v3")) {
                    bundles.add(new Bundle(symbolicName, attributes));
                }
            }
        }
        return bundles;
    }

    // read from this module's own pom.xml so the expected count never drifts from the actual dependency list
    private static int declaredJdbiTestDependencyCount() throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("pom.xml"));
        NodeList dependencies = doc.getElementsByTagName("dependency");
        int count = 0;
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if ("org.jdbi".equals(childText(dependency, "groupId"))
                    && childText(dependency, "artifactId").startsWith("jdbi3-")) {
                count++;
            }
        }
        return count;
    }

    private static String childText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }
}
