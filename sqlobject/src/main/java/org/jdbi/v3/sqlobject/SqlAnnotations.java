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

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

import org.jdbi.v3.internal.JdbiOptionals;

public class SqlAnnotations {
    // TODO move to locator package, make package private
    public static Optional<String> getAnnotationValue(Method method) {
        Predicate<String> nonEmpty = s -> !s.isEmpty();

        return JdbiOptionals.findFirstPresent(
                () -> Optional.ofNullable(method.getAnnotation(SqlBatch.class)).map(SqlBatch::value).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlCall.class)).map(SqlCall::value).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlQuery.class)).map(SqlQuery::value).filter(nonEmpty),
                () -> Optional.ofNullable(method.getAnnotation(SqlUpdate.class)).map(SqlUpdate::value).filter(nonEmpty));
    }

    static String getSql(SqlCall q, Method m) {
        String value = q.value();
        if (value.isEmpty()) {
            return m.getName();
        } else {
            return value;
        }
    }

    static String getSql(SqlQuery q, Method m) {
        String value = q.value();
        if (value.isEmpty()) {
            return m.getName();
        } else {
            return value;
        }
    }

    static String getSql(SqlUpdate q, Method m) {
        String value = q.value();
        if (value.isEmpty()) {
            return m.getName();
        } else {
            return value;
        }
    }

    static String getSql(SqlBatch q, Method m) {
        String value = q.value();
        if (value.isEmpty()) {
            return m.getName();
        } else {
            return value;
        }
    }
}
