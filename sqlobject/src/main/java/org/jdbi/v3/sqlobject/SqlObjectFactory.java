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

import java.util.stream.Stream;

/**
 * Creates implementations for SqlObject interfaces.
 */
public final class SqlObjectFactory extends AbstractSqlObjectFactory {

    public static final String EXTENSION_ID = "SQL_OBJECT";

    SqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {
        // only allow interfaces.
        if (!extensionType.isInterface()) {
            return false;
        }

        // ignore generator types
        if (isConcrete(extensionType)) {
            return false;
        }

        // extending SqlObject is ok
        if (SqlObject.class.isAssignableFrom(extensionType)) {
            return true;
        }

        // otherwise at least one method must be marked with a SqlOperation or UseExtensionMethod with the SQL id.
        return Stream.of(extensionType.getMethods())
                .flatMap(m -> Stream.of(m.getAnnotations()))
                .anyMatch(SqlObjectAnnotationHelper::matchSqlAnnotations);
    }
}
