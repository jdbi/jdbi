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

import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public interface ParameterCustomizerFactory {
    /**
     * Creates parameter customizer used to bind sql statement parameters
     * when parameter is not explicitly annotated
     *
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was identified as an SQL method
     * @param param the parameter to bind
     * @param index the method parameter index
     * @return the customizer which will be applied to the generated statement
     */
    SqlStatementParameterCustomizer createForParameter(Class<?> sqlObjectType,
                                                       Method method,
                                                       Parameter param,
                                                       int index);
}
