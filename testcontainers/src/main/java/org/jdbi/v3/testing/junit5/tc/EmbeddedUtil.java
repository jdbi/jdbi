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

package org.jdbi.v3.testing.junit5.tc;

import java.util.concurrent.ThreadLocalRandom;

final class EmbeddedUtil {

    private static final String LOWERCASE;

    static {
        LOWERCASE = sequence('a', 26);
    }

    private EmbeddedUtil() {
        throw new AssertionError("EmbeddedUtil can not be instantiated");
    }

    static String randomLowercase(int length) {
        return randomString(LOWERCASE, length);
    }

    private static String sequence(char start, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append((char) (start + i));
        }
        return sb.toString();
    }

    private static String randomString(String alphabet, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int random = ThreadLocalRandom.current().nextInt(alphabet.length());
            sb.append(alphabet.charAt(random));
        }
        return sb.toString();
    }
}
