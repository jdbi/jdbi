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

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Standard implementation of {@link DefaultParameterCustomizerFactory} that delegates to
 * {@link Bind.Factory#createForParameter(Annotation, Class, Method, Parameter, int)} passing null for Annotation parameter.
 */
public class BindFactoryDefaultParameterCustomizerFactory implements DefaultParameterCustomizerFactory {

    private SqlStatementCustomizerFactory bindSqlStatementCustomizerFactory = new Bind.Factory();

    @Override
    public SqlStatementParameterCustomizer createForParameter(Class<?> sqlObjectType, Method method, Parameter param, int index) {
        return bindSqlStatementCustomizerFactory.createForParameter(null, sqlObjectType, method, param, index);
    }
}
