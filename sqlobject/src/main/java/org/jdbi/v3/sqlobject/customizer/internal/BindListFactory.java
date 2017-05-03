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
package org.jdbi.v3.sqlobject.customizer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import org.jdbi.v3.core.internal.IterableLike;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

public final class BindListFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        final BindList bindList = (BindList) annotation;
        final String name = ParameterUtil.getParameterName(bindList, bindList.value(), param);

        return (stmt, arg) -> {
            if (arg == null || IterableLike.isEmpty(arg)) {
                switch (bindList.onEmpty()) {
                case VOID:
                    stmt.define(name, "");
                    return;
                case NULL:
                    stmt.define(name, "null");
                    return;
                case THROW:
                    throw new IllegalArgumentException(arg == null
                    ? "argument is null; null was explicitly forbidden on this instance of BindList"
                            : "argument is empty; emptiness was explicitly forbidden on this instance of BindList");
                default:
                    throw new IllegalStateException(EmptyHandling.valueNotHandledMessage);
                }
            }

            stmt.bindList(name, IterableLike.toList(arg));
        };
    }
}
