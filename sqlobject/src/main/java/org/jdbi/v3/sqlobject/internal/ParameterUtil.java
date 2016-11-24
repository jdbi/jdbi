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
package org.jdbi.v3.sqlobject.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class ParameterUtil {
    public static String getParameterName(Annotation annotation,
                                          String nameFromAnnotation,
                                          Parameter parameter) {
        if (!nameFromAnnotation.isEmpty()) {
            return nameFromAnnotation;
        }
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        String annotationTypeName = annotation == null ? "" : "@" + annotation.annotationType().getSimpleName() + " ";
        throw new UnsupportedOperationException("A " + annotationTypeName + "parameter was not given a name, "
                + "and parameter name data is not present in the class file, for: "
                + parameter.getDeclaringExecutable() + " :: " + parameter);
    }
}
