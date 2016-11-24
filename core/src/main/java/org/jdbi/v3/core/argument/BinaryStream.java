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

import java.io.InputStream;

/**
 * Parameter wrapper type for input stream arguments that should be bound as binary.
 */
public class BinaryStream {
    private final InputStream stream;
    private final int length;

    public BinaryStream(InputStream stream, int length) {
        this.stream = stream;
        this.length = length;
    }

    public InputStream getStream() {
        return stream;
    }

    public int getLength() {
        return length;
    }
}
