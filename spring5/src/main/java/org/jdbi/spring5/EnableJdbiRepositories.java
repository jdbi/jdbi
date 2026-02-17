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
package org.jdbi.spring5;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Annotating a spring configuration class with this annotation enables the scanning/detection of jdbi repositories.
 * The scanned packages can be configured in the annotation. If no explicit configuration is done the package of the
 * annotated element will be used as the sole base package.
 *
 * @deprecated Use the {@link org.jdbi.v3.spring} module with Spring 6.x or newer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(JdbiRepositoryRegistrar.class)
@Deprecated(forRemoval = true, since = "3.47.0")
public @interface EnableJdbiRepositories {

    /**
     * The names of the base packages used for repository scanning in addition
     * to the {@link #basePackages} and {@link #basePackageClasses} properties.
     */
    String[] value() default {};

    /**
     * The names of the base packages used for repository scanning in addition
     * to the {@link #value} and {@link #basePackageClasses} properties.
     */
    String[] basePackages() default {};

    /**
     * The packages of these classes are used as base packages for repository
     * scanning in addition to the {@link #value} and {@link #basePackages}
     * properties.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Exact array of classes to consider as repositories. Overriding any of
     * the values defined in {@link #value}, {@link #basePackages} or
     * {@link #basePackageClasses}.
     */
    Class<?>[] repositories() default {};

}
