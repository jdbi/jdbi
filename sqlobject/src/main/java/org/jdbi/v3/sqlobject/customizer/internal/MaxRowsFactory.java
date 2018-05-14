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
import java.text.MessageFormat;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.customizer.MaxRows;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

public class MaxRowsFactory implements SqlStatementCustomizerFactory {
    // arbitrary number to avoid coincidences
    public static final int DEFAULT_MAX_ROWS = -1;

    @Override
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        final int maxRows = ((MaxRows) annotation).value();

        if (maxRows == DEFAULT_MAX_ROWS) {
            throw new IllegalArgumentException(MessageFormat.format(
                "no value given for @{0} on {1}:{2}",
                MaxRows.class.getSimpleName(),
                sqlObjectType.getName(),
                method.getName())
           );
        }
        if (maxRows <= 0) {
            throw new IllegalArgumentException(MessageFormat.format(
                "@{0} value given on {1}:{2} is {3}, which is negative or 0. This makes no sense.",
                MaxRows.class.getSimpleName(),
                sqlObjectType.getName(),
                method.getName(),
                maxRows)
           );
        }

        return stmt -> ((Query) stmt).setMaxRows(maxRows);
    }

    /*
    when used on a parameter, we use the parameter value and ignore the annotation's value field
     */
    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        int value = ((MaxRows) annotation).value();
        if (value != DEFAULT_MAX_ROWS) {
            throw new IllegalArgumentException(MessageFormat.format(
                "You''ve specified a value for @{0} on {1}:{2}({3}) — this value won''t do anything, the parameter value will be used instead. Remove the value to prevent confusion.",
                MaxRows.class.getSimpleName(),
                sqlObjectType.getName(),
                method.getName(),
                param.getName())
           );
        }

        return (stmt, arg) -> {
            int maxRows = (int) arg;
            if (maxRows <= 0) {
                throw new IllegalArgumentException(MessageFormat.format(
                    "@{0} value given on {1}:{2}({3}) is {4}, which is negative or 0. This makes no sense.",
                    MaxRows.class.getSimpleName(),
                    sqlObjectType.getName(),
                    method.getName(),
                    param.getName(),
                    maxRows)
               );
            }

            ((Query) stmt).setMaxRows(maxRows);
        };
    }
}
