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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A binding annotation.  Will bind both as the {@code value}
 * of the annotation as well as the parameter index.  If the
 * {@code value} is not specified, it will default to using the parameter
 * name.  It is an error to not specify the name when there is no
 * parameter naming information in your class files.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindFactory.class)
public @interface Bind
{
    String USE_PARAM_NAME = "___use_param_name___";

    String value() default USE_PARAM_NAME;

    Class<? extends Binder<Bind, ?>> binder() default DefaultObjectBinder.class;
}
