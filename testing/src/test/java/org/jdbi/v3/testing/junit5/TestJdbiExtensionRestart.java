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
package org.jdbi.v3.testing.junit5;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * A static {@link JdbiExtension} is started and stopped once per execution of its test class. When the same
 * class executes a second time in the same JVM, for example a Surefire rerun of a failed test or a JUnit
 * {@code @Nested} class selected as its own test class, the extension object is reused and must come back with
 * the plugins and the initializer it was configured with.
 */
public class TestJdbiExtensionRestart {

    @Test
    public void staticExtensionKeepsPluginsAndInitializerAcrossRuns() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = request().selectors(selectClass(PluginUser.class)).build();

        for (int run = 1; run <= 2; run++) {
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.execute(request, listener);
            TestExecutionSummary summary = listener.getSummary();
            assertThat(summary.getTestsFoundCount()).as("run %d found", run).isEqualTo(2);
            assertThat(summary.getFailures()).as("run %d failures", run).isEmpty();
        }
    }

    // Executed through the launcher above only; Surefire skips inner classes by default.
    public static class PluginUser {

        @RegisterExtension
        public static JdbiExtension h2 = JdbiExtension.h2()
            .withPlugin(new MarkerPlugin())
            .withInitializer((ds, handle) -> handle.execute("CREATE TABLE marker (v VARCHAR)"));

        @Test
        public void pluginIsInstalled() {
            Handle handle = h2.getSharedHandle();
            assertThat(handle.createQuery("SELECT 'x'").mapTo(Marker.class).one()).isEqualTo(new Marker("x"));
        }

        @Test
        public void initializerRan() {
            Handle handle = h2.getSharedHandle();
            assertThat(handle.createQuery("SELECT COUNT(1) FROM marker").mapTo(Integer.class).one()).isZero();
        }
    }

    record Marker(String value) {}

    static class MarkerPlugin implements JdbiPlugin {
        @Override
        public void customizeJdbi(Jdbi jdbi) {
            jdbi.registerColumnMapper(Marker.class, (rs, col, ctx) -> new Marker(rs.getString(col)));
        }
    }
}
