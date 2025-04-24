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

import java.lang.annotation.Annotation;

import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.internal.UtilityClassException;

import static org.jdbi.v3.sqlobject.SqlObjectFactory.EXTENSION_ID;

final class SqlObjectAnnotationHelper {

    private SqlObjectAnnotationHelper() {
        throw new UtilityClassException();
    }

    static boolean matchSqlAnnotations(Annotation a) {
        UseExtensionHandler extensionHandlerAnnotation = a.annotationType().getAnnotation(UseExtensionHandler.class);
        return extensionHandlerAnnotation != null && EXTENSION_ID.equals(extensionHandlerAnnotation.id());
    }
}
