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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to control the batch chunk size for sql batch operations.
 * If this annotation is present the value (or argument value if on
 * a parameter) will be used as the size for each batch statement to
 * execute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface BatchChunkSize
{
    /**
     * The batch chunk size. Defaults to -1 which will raise an error, so
     * do not use the default. It is present for when the annotation is used
     * on a parameter, in which case this value will be ignored and the parameter value
     * will be used. The parameter type must be an int (or castable to an int).
     * @return the batch chunk size.
     */
    int value() default -1;
}
