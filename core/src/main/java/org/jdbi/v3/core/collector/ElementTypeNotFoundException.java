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
package org.jdbi.v3.core.collector;

import org.jdbi.v3.core.JdbiException;

/**
 * Thrown when jdbi tries to build a Collector, but cannot determine the element
 * type intended for it.
 */
@SuppressWarnings("serial")
public class ElementTypeNotFoundException extends JdbiException {
    public ElementTypeNotFoundException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ElementTypeNotFoundException(Throwable cause) {
        super(cause);
    }

    public ElementTypeNotFoundException(String message) {
        super(message);
    }
}
