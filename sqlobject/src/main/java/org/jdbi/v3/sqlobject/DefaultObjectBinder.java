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
import java.lang.reflect.Type;
import java.util.Iterator;

import org.jdbi.v3.PreparedBatchPart;
import org.jdbi.v3.SqlStatement;
import org.jdbi.v3.Types;

class DefaultObjectBinder implements Binder<Bind, Object>
{
    DefaultObjectBinder()
    {
    }

    @Override
    public void bind(SqlStatement<?> q, Parameter param, int index, Bind b, Object arg)
    {
        final String bindName;
        if (b == null || b.value().equals(Bind.USE_PARAM_NAME)) {
            if (param.isNamePresent()) {
                bindName = param.getName();
            } else {
                throw new UnsupportedOperationException("A parameter was not given a name, "
                        + "and parameter name data is not present in the class file, for: "
                        + param.getDeclaringExecutable() + " :: " + param);
            }
        } else {
            bindName = b.value();
        }

        Type type = param.getParameterizedType();

        if (q instanceof PreparedBatchPart) {
            // FIXME BatchHandler should extract the iterable/iterator element type and pass it to the binder
            Class<?> erasedType = Types.getErasedType(type);
            if (Iterable.class.isAssignableFrom(erasedType)) {
                type = Types.findGenericParameter(type, Iterable.class).get();
            }
            else if (Iterator.class.isAssignableFrom(erasedType)) {
                type = Types.findGenericParameter(type, Iterator.class).get();
            }
        }

        q.bindByType(index, arg, type);
        q.bindByType(bindName, arg, type);
    }
}
