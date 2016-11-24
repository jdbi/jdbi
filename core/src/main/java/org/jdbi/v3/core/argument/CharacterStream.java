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
package org.jdbi.v3.core.argument;

import java.io.Reader;

public class CharacterStream {
    private final Reader reader;
    private final int length;

    public CharacterStream(Reader reader, int length) {
        this.reader = reader;
        this.length = length;
    }

    public Reader getReader() {
        return reader;
    }

    public int getLength() {
        return length;
    }
}
