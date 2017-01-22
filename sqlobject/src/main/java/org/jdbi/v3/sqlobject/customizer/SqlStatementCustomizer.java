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
package org.jdbi.v3.sqlobject.customizer;

import org.jdbi.v3.core.statement.SqlStatement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;

/**
 * Customizes {@link SqlStatement} instances based on the presence of declarative annotations on a SQL object type,
 * method, or method parameter.
 */
public interface SqlStatementCustomizer
{
    /**
     * Customize the SQL statement according to the annotation on the SQL object type.
     *
     * @param statement the {@link SqlStatement} to customize.
     * @param annotation the annotation which led to the method being called
     * @param sqlObjectType sql object class (interface) which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    default void customizeForType(SqlStatement<?> statement,
                                  Annotation annotation,
                                  Class<?> sqlObjectType) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Customize the SQL statement according to the annotation on the SQL object method
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    default void customizeForMethod(SqlStatement<?> statement,
                                    Annotation annotation,
                                    Class<?> sqlObjectType,
                                    Method method) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported for method");
    }

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param param the parameter which was annotated
     * @param index the method parameter index
     * @param arg the argument value for the annotated parameter
     * @return the customizer which will be applied to the generated statement
     */
    default void customizeForParameter(SqlStatement<?> statement,
                                       Annotation annotation,
                                       Class<?> sqlObjectType,
                                       Method method,
                                       Parameter param, // TODO could this be elided?
                                       int index,
                                       Object arg) throws SQLException
    {
        throw new UnsupportedOperationException("Not supported for parameter");
    }
}
