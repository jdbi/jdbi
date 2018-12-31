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
        final String name = ParameterUtil.findParameterName(bindList.value(), param)
                .orElseThrow(() -> new UnsupportedOperationException("A @BindList parameter was not given a name, "
                        + "and parameter name data is not present in the class file, for: "
                        + param.getDeclaringExecutable() + "::" + param));

        return (stmt, arg) -> {
            if (arg == null || IterableLike.isEmpty(arg)) {
                bindList.onEmpty().define(stmt, name);
            } else {
                stmt.bindList(name, IterableLike.toList(arg));
            }
        };
    }
}
