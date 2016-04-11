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
package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to build customizing annotations. Use this to annotate an annotation. See examples
 * in the org.skife.jdbi.v2.sqlobject.customizers package.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlStatementCustomizingAnnotation
{
    /**
     * Specify a sql statement customizer factory which will be used to create
     * sql statement customizers.
     * @return a factory used to crate customizers for the customizing annotation
     */
    Class<? extends SqlStatementCustomizerFactory> value();
}
