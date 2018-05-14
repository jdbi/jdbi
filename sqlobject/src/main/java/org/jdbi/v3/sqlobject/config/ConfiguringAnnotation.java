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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Annotation used to modify configuration in the context of a SQL object or method. Use this to annotate an annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ConfiguringAnnotation {
    /**
     * A {@link Configurer} type, which will be used to configure {@link ConfigRegistry} instances.
     *
     * @return the Configurer type used to configure a {@link ConfigRegistry} for the configuring annotation.
     */
    Class<? extends Configurer> value();
}
