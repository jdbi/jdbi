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
package org.jdbi.v3.core.internal;

/**
 * Reports whether the code runs in a GraalVM native image. The GraalVM SDK is not on the class path, so
 * this reads the system property that the image sets, which is what JUnit does for
 * {@code @DisabledInNativeImage}.
 */
public final class NativeImageDetector {

    private static final String IMAGE_CODE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    private NativeImageDetector() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * @return true if the caller runs in a GraalVM native image, either while the image is built or after
     */
    public static boolean inNativeImage() {
        return System.getProperty(IMAGE_CODE_PROPERTY) != null;
    }
}
