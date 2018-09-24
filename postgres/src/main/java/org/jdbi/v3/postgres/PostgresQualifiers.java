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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.meta.Beta;

/**
 * Constants class for type qualifying annotations supported by Jdbi Postgres plugin.
 */
@Beta
public class PostgresQualifiers {
    private PostgresQualifiers() {}

    /**
     * Returns an {@link HStore} qualifying annotation instance.
     */
    public static HStore hStore() {
        return AnnotationFactory.create(HStore.class);
    }

    /**
     * Returns a {@link MacAddr} qualifying annotation instance
     */
    public static MacAddr macAddr() {
        return AnnotationFactory.create(MacAddr.class);
    }
}
