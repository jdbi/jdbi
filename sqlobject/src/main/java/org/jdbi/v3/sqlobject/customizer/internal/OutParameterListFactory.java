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
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.sqlobject.customizer.OutParameter;
import org.jdbi.v3.sqlobject.customizer.OutParameterList;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;

public class OutParameterListFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        final OutParameterList outParams = (OutParameterList) annotation;

        return stmt -> {
            Call call = (Call) stmt;
            for (OutParameter outParam : outParams.value()) {
                call.registerOutParameter(outParam.name(), outParam.sqlType());
            }
        };
    }
}
