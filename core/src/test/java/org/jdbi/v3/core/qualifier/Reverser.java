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
package org.jdbi.v3.core.qualifier;

public class Reverser {
    private Reverser() {}

    public static String reverse(String s) {
        StringBuilder b = new StringBuilder(s.length());

        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
        }

        return b.toString();
    }
}
