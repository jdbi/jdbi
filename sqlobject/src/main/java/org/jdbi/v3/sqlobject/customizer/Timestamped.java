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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;

/**
 * Binds the named parameter <code>:now</code> or a custom named parameter with
 * the current DateTime as an {@link OffsetDateTime}.
 * Common use cases:
 * <pre>
 * <code>
 * public interface PersonDAO {
 *      &#64;SqlUpdate("INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :now, :now)")
 *      &#64;Timestamped
 *      &#64;GetGeneratedKeys
 *      int insert(@BindBean("p") Person person);
 *
 *      &#64;SqlUpdate("UPDATE people SET modified = :now, firstName = :p.firstName, lastName = :p.lastName, email = :p.email WHERE id = :p.id")
 *      &#64;Timestamped
 *      void update(@BindBean("p") Person person);
 *  }
 * </code>
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(Timestamped.Factory.class)
@Documented
public @interface Timestamped {
    /**
     * The parameter to bind in the SQL query. If omitted, defaults to <code>now</code>
     * and can be changed to customize the parameter bound to the current DateTime
     *
     * @return the parameter name
     */
    String value() default "now";

    class Factory implements SqlStatementCustomizerFactory {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            final String parameterName = ((Timestamped) annotation).value();

            return q -> {
                q.bind(parameterName, OffsetDateTime.now());
            };
        }
    }
}
