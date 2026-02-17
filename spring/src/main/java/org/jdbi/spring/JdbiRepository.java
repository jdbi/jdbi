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
package org.jdbi.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When {@link EnableJdbiRepositories} is used,
 * detected interfaces with this annotation will be regarded as a jdbi (sql-object) repository
 * and are elligible for autowiring. The handle used for the execution is obtained and discarded using {@link JdbiUtil}
 * and consequently will work in combination with spring managed transactions.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JdbiRepository {
    /**
     * The name of the created bean. if omitted the default naming mechanism is used.
     */
    String value() default "";

    /**
     * The qualifier (bean name) of the jdbi to use. Can be omitted if only one jdbi bean is available.
     */
    String jdbiQualifier() default "";
}
