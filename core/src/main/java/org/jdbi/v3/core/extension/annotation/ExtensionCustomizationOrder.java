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
package org.jdbi.v3.core.extension.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.meta.Alpha;

/**
 * Determines the order in which extension method decorators are invoked. If this annotation is absent, the decorator order
 * is undefined. A <code>@ExtensionCustomizationOrder</code> annotation on a method takes precedence over an annotation on a type.
 *
 * @since 3.38.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Alpha
public @interface ExtensionCustomizationOrder {
    /**
     * The order that decorator annotations will be applied, from outermost to innermost. Decorator order is undefined
     * for any decorating annotation present on a method but not on this annotation.
     * @return the annotations in the order defined.
     */
    Class<? extends Annotation>[] value();
}
