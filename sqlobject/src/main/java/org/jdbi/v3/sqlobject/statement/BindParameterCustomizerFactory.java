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
package org.jdbi.v3.sqlobject.statement;

import java.lang.reflect.Type;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.jdbi.v3.sqlobject.customizer.internal.BindFactory;

/**
 * Standard implementation of {@link ParameterCustomizerFactory} that delegates to
 * {@link BindFactory#createForParameter(Annotation, Class, Method, Parameter, int, Type)} passing null for Annotation parameter.
 */
public class BindParameterCustomizerFactory implements ParameterCustomizerFactory {

    private SqlStatementCustomizerFactory bindSqlStatementCustomizerFactory = new BindFactory();

    @Override
    public SqlStatementParameterCustomizer createForParameter(Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        return bindSqlStatementCustomizerFactory.createForParameter(null, sqlObjectType, method, param, index, type);
    }
}
