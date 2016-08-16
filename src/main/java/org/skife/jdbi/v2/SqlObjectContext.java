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
package org.skife.jdbi.v2;

import java.lang.reflect.Method;

public class SqlObjectContext {
    public final Class<?> type;
    public final Method method;

    public SqlObjectContext() {
        this(null, null);
    }

    public SqlObjectContext(Class<?> type, Method method) {
        this.type = type;
        this.method = method;
    }
}
