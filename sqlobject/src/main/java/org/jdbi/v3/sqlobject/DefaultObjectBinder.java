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

import java.lang.reflect.Type;
import java.util.Iterator;

import org.jdbi.v3.core.PreparedBatchPart;
import org.jdbi.v3.core.util.GenericTypes;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

class DefaultObjectBinder implements BinderFactory<Bind, Object>
{
    DefaultObjectBinder()
    {
    }

    @Override
    public Binder<Bind, Object> build(Bind annotation)
    {
        return (q, param, index, b, arg) ->
        {
            String nameFromAnnotation = b == null ? "" : b.value();
            final String name = ParameterUtil.getParameterName(b, nameFromAnnotation, param);

            Type type = param.getParameterizedType();

            if (q instanceof PreparedBatchPart) {
                // FIXME BatchHandler should extract the iterable/iterator element type and pass it to the binder
                Class<?> erasedType = GenericTypes.getErasedType(type);
                if (Iterable.class.isAssignableFrom(erasedType)) {
                    type = GenericTypes.findGenericParameter(type, Iterable.class).get();
                }
                else if (Iterator.class.isAssignableFrom(erasedType)) {
                    type = GenericTypes.findGenericParameter(type, Iterator.class).get();
                }
            }

            q.bindByType(index, arg, type);
            q.bindByType(name, arg, type);
        };
    }
}
