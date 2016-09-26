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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

/**
 * Used to set attributes on the StatementContext for the statement generated for this method.
 * These values will be available to other customizers, such as the statement locator or rewriter.
 */
@SqlStatementCustomizingAnnotation(Define.Factory.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Define
{
    /**
     * The key for the attribute to set. The value will be the value passed to the annotated argument
     * @return the attribute key
     */
    String value() default "";

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, final Object arg)
        {
            Define define = (Define) annotation;

            String name = define.value();
            if (name.isEmpty()) {
                if (param.isNamePresent()) {
                    name = param.getName();
                } else {
                    throw new UnsupportedOperationException("A @Define parameter was not given a name, "
                            + "and parameter name data is not present in the class file, for: "
                            + param.getDeclaringExecutable() + " :: " + param);
                }
            }

            final String key = name;
            return q -> q.define(key, arg);
        }
    }
}
