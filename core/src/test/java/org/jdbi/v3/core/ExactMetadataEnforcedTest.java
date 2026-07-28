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
package org.jdbi.v3.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Proves that {@code --exact-reachability-metadata} is in force when the build says it is, so that a
 * passing native run means the metadata was checked rather than that the check was absent. Without
 * this, dropping the flag, misspelling a package or losing the profile would turn the whole suite
 * green for the wrong reason.
 *
 * <p>Both outcomes are asserted rather than one of them skipped, so the build's own account of the
 * flag has to match what the image does. A run with the flag off is therefore still worth something:
 * it fails if the flag turns out to be on.
 *
 * <p>The flag matches the package of the code performing the lookup, and only packages that exist in
 * the source tree are named, so this has to live in a package that ships main sources. That is why a
 * native-image concern sits in the core tests rather than in the native-image test module, whose own
 * package is deliberately not part of the checked scope.
 *
 * <p>What a passing suite establishes is narrower than it looks. A lookup whose receiver native-image
 * can determine while building the image is answered there, and answering it also brings that type's
 * metadata into the image, which then satisfies every other lookup of the same type, including the
 * ones that stayed dynamic. So a type can need no entry of its own purely because some unrelated
 * reachable code mentions it as a class literal, and a green run means no gaps among the lookups that
 * remain dynamic rather than no gaps at all.
 */
class ExactMetadataEnforcedTest {

    private static final String FLAG_PROPERTY = "jdbi.native.exact-metadata";

    @Test
    void unregisteredLookupIsRejected() {
        // Assembled through a call rather than written out, because native-image resolves a constant
        // name while building the image and the lookup then never reaches the check. An earlier
        // version derived the class from an allocation, whose type is statically known, and passed
        // while proving nothing. If this is ever folded the assertions below fail rather than
        // quietly succeeding, so the check cannot rot into a no-op unnoticed.
        String unregistered = String.join(".", "org", "jdbi", "v3", "core", "NoSuchTypeForMetadataProbe");

        Throwable thrown = catchThrowable(() -> Class.forName(unregistered));

        assertThat(thrown)
                .describedAs("%s must not resolve. If a type of that name is ever added and"
                        + " registered, this test silently stops testing anything.", unregistered)
                .isNotNull();

        if (exactMetadataEnforced()) {
            // A type that is absent still has to be registered for the image to answer "absent", so
            // an unregistered name is rejected rather than reported as ClassNotFoundException.
            assertThat(thrown.getClass().getName())
                    .describedAs("An unregistered lookup of %s must be rejected. Anything else means"
                            + " exact reachability metadata is not being enforced, and a green native"
                            + " run does not mean the metadata is complete.", unregistered)
                    .contains("MissingReflectionRegistration");
        } else {
            assertThat(thrown)
                    .describedAs("Without exact reachability metadata the lookup is answered as"
                            + " absent. A missing registration error here means the flag is in force"
                            + " although this build reports it as off.")
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    private static boolean exactMetadataEnforced() {
        if (System.getProperty("org.graalvm.nativeimage.imagecode") == null) {
            return false;
        }
        // A native build always states which mode it built, so an absent value is a broken build
        // rather than a mode. Left unchecked it would read as "off" and quietly assert the opposite
        // of what the image does.
        String flag = System.getProperty(FLAG_PROPERTY);
        assertThat(flag)
                .describedAs("A native build must say whether it enabled exact reachability metadata."
                        + " If %s is absent, the surefire configuration in native-tests/pom.xml that"
                        + " passes it is gone, and this test can no longer tell the two cases apart.",
                        FLAG_PROPERTY)
                .isNotNull();
        return Boolean.parseBoolean(flag);
    }
}
