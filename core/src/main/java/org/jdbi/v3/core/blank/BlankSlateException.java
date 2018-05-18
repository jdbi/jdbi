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
package org.jdbi.v3.core.blank;

import java.lang.reflect.Type;
import java.text.MessageFormat;

public class BlankSlateException extends UnsupportedOperationException {
    public BlankSlateException(Class<?> function, Type type) {
        super(MessageFormat.format("BlankSlate {0} for type {1}", function, type));
    }

    public BlankSlateException(Class<?> function, Type type, Object value) {
        super(MessageFormat.format("BlankSlate {0} for type {1} and value {2}", function, type, value));
    }
}
