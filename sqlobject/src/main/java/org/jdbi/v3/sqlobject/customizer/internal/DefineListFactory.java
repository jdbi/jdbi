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
import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.DefineList;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.internal.ParameterUtil;

public final class DefineListFactory implements SqlStatementCustomizerFactory {

    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        final DefineList d = (DefineList) annotation;
        final String name = ParameterUtil.findParameterName(d.value(), param)
                .orElseThrow(() -> new UnsupportedOperationException("A @DefineList parameter was not given a name, "
                        + "and parameter name data is not present in the class file, for: "
                        + param.getDeclaringExecutable() + "::" + param));

        return (stmt, arg) -> {
            List<?> argsList;
            if (arg instanceof List) {
                argsList = (List<?>) arg;
            } else if (arg instanceof Object[]) {
                argsList = Arrays.asList((Object[]) arg);
            } else if (arg == null) {
                throw new IllegalArgumentException("A null object was passed as a @DefineList parameter. "
                        + "@DefineList is only supported on List and array arguments");
            } else {
                throw new IllegalArgumentException("A " + arg.getClass() + " object was passed as a @DefineList "
                        + "parameter. @DefineList is only supported on List and array arguments");
            }
            if (argsList.isEmpty()) {
                throw new IllegalArgumentException("An empty list was passed as a @DefineList parameter. Can't define "
                        + "an empty attribute.");
            }
            if (argsList.contains(null)) {
                throw new IllegalArgumentException("A @DefineList parameter was passed a list with null values in it.");
            }

            stmt.defineList(name, argsList);
        };
    }
}
