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
package org.jdbi.v3.nativetests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the package list that scopes {@code --exact-reachability-metadata}.
 *
 * <p>That flag matches the package of the code performing a reflective lookup. A parent package does
 * not cover its subpackages and wildcards are rejected, so every package has to be named. A package
 * added later and not named here would silently stop being checked, which is the failure this test
 * exists to prevent: it compares the configured list against the packages actually present in the
 * source tree and fails on any difference in either direction.
 */
class ExactMetadataPackagesTest {

    private static final String CONFIGURED = "jdbi.native.exact-metadata.packages";
    private static final String REACTOR_ROOT = "jdbi.reactor.root";
    private static final Pattern SOURCE_FILE =
            Pattern.compile(".*/src/main/(?:java|kotlin)/(org/jdbi/.*)/[^/]*\\.(?:java|kt)");

    @Test
    void configuredPackagesMatchTheSourceTree() throws IOException {
        // the scan reads the source tree, which a native image does not carry. AssertJ's own
        // assumptions define a class at run time, which a native image forbids, so use JUnit's.
        String configured = System.getProperty(CONFIGURED);
        String root = System.getProperty(REACTOR_ROOT);
        assumeTrue(configured != null && root != null, CONFIGURED + " is only set by a JVM build");

        assertThat(scanSourceTree(Path.of(root)))
                .describedAs("Packages named in %s must match the source tree exactly. Regenerate the"
                        + " property in internal/build/pom.xml when adding or removing a package.",
                        CONFIGURED)
                .isEqualTo(split(configured));
    }

    private static Set<String> split(String value) {
        return value == null ? new TreeSet<>() : Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(toCollection(TreeSet::new));
    }

    private static Set<String> scanSourceTree(Path root) throws IOException {
        try (Stream<Path> sources = Files.walk(root)) {
            return sources
                    .map(root::relativize)
                    // build output, and hidden directories such as the git worktrees under .claude,
                    // can hold a second checkout whose packages are not this build's packages
                    .filter(p -> stream(p.spliterator(), false)
                            .map(Path::toString)
                            .noneMatch(name -> "target".equals(name) || name.startsWith(".")))
                    .map(Path::toString)
                    .filter(p -> SOURCE_FILE.matcher(p).matches())
                    .map(p -> SOURCE_FILE.matcher(p).replaceFirst("$1"))
                    .map(p -> p.replace('/', '.'))
                    // the demo is a standalone project, not part of the reactor
                    .filter(p -> !p.startsWith("org.jdbi.nativedemo"))
                    .collect(toCollection(TreeSet::new));
        }
    }
}
