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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.SqlStatement;

class Bindifier<AnnoType extends Annotation>
{
    private final int    param_idx;
    private final Binder<AnnoType, Object> binder;
    private final AnnoType annotation;
    private final Parameter parameter;

    Bindifier(Method method, AnnoType bind, int param_idx, Binder<AnnoType, Object> binder)
    {
        this.annotation = bind;
        this.param_idx = param_idx;
        this.binder = binder;
        this.parameter = method.getParameters()[param_idx];
    }

    void bind(SqlStatement<?> q, Object[] args)
    {
        binder.bind(q, parameter, param_idx, annotation, args[param_idx]);
    }
}
