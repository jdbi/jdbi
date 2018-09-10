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

import java.lang.annotation.Annotation;

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
        return new HStore() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return HStore.class;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof HStore;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "@org.jdbi.v3.postgres.HStore()";
            }
        };
    }

    /**
     * Returns a {@link MacAddr} qualifying annotation instance
     */
    public static MacAddr macAddr() {
        return new MacAddr() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return MacAddr.class;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof MacAddr;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "@org.jdbi.v3.postgres.MacAddr()";
            }
        };
    }
}
