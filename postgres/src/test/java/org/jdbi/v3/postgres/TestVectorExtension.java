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

import com.pgvector.PGhalfvec;
import com.pgvector.PGsparsevec;
import com.pgvector.PGvector;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PgVectorContainerProvider;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestVectorExtension {
    @Container
    static JdbcDatabaseContainer<?> dbContainer = new PgVectorContainerProvider().newInstance("pg17");

    @RegisterExtension
    static JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
            .withPlugin(new PostgresPlugin());

    @BeforeAll
    static void createVectorExtension() {
        extension.getSharedHandle().execute("create extension if not exists vector");
    }

    @Test
    void testVector() {
        try (var h = extension.openHandle()) {
            h.execute("create table vectors (id bigserial primary key, embedding vector(3))");
            final var expected = new PGvector(new float[] { 2, 4, 8 });
            h.execute("insert into vectors (embedding) values (?)", expected);
            final var actual = h.createQuery("select embedding from vectors")
                    .mapTo(PGvector.class)
                    .one();

            assertThat(actual.toArray()).isEqualTo(expected.toArray());
        }
    }

    @Test
    void testHalfVector() {
        try (var h = extension.openHandle()) {
            h.execute("create table halfvecs (id bigserial primary key, embedding halfvec(3))");
            final var expected = new PGhalfvec(new float[] { 2, 4, 8 });
            h.execute("insert into halfvecs (embedding) values (?)", expected);
            final var actual = h.createQuery("select embedding from halfvecs")
                    .mapTo(PGhalfvec.class)
                    .one();

            assertThat(actual.toArray()).isEqualTo(expected.toArray());
        }
    }

    @Test
    void testSparseVector() {
        try (var h = extension.openHandle()) {
            h.execute("create table sparsevecs (id bigserial primary key, embedding sparsevec(3))");
            final var expected = new PGsparsevec(new float[] { 2, 0, 8 });
            h.execute("insert into sparsevecs (embedding) values (?)", expected);
            final var actual = h.createQuery("select embedding from sparsevecs")
                    .mapTo(PGsparsevec.class)
                    .one();

            assertThat(actual.toArray()).isEqualTo(expected.toArray());
        }
    }
}
