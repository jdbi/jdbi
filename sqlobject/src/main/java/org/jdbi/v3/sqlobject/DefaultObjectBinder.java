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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Parameter;

import org.jdbi.v3.SQLStatement;

class DefaultObjectBinder implements Binder<Bind, Object>
{
    @Override
    public void bind(SQLStatement<?> q, Parameter param, Bind b, Object arg)
    {
        String value = b.value();

        if (value.equals(Bind.USE_PARAM_NAME)) {
            if (!param.isNamePresent()) {
                throw new UnsupportedOperationException("A parameter was annotated with @Bind "
                        + "but no name was specified, and parameter name data is not present "
                        + "in the class file.  " + param.getDeclaringExecutable() + " :: " + param);
            }
            value = param.getName();
        }

        q.bind(value, arg);
    }
}
