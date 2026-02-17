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
package org.jdbi.sqlobject.statement.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import org.jdbi.core.generic.GenericType;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer;

public class MapToFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        ResultReturner returner = ResultReturner.forMethod(sqlObjectType, method);
        return (sqlStatement, arg) -> {
            final QualifiedType<?> mapTo;

            if (arg instanceof QualifiedType<?> qt) {
                mapTo = qt;
            } else if (arg instanceof GenericType<?> gt) {
                mapTo = QualifiedType.of(gt.getType());
            } else if (arg instanceof Type t) {
                mapTo = QualifiedType.of(t);
            } else {
                throw new UnsupportedOperationException("@MapTo must take a GenericType, QualifiedType, or Type, but got a " + arg.getClass().getName());
            }

            sqlStatement.getConfig(SqlObjectStatementConfiguration.class).setReturner(
                    () -> returner.mappedResult(((ResultBearing) sqlStatement).mapTo(mapTo), sqlStatement.getContext()));
        };
    }
}
