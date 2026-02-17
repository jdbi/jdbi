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
package org.jdbi.core;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.assertj.core.api.Assertions;
import org.jdbi.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestHandlePg {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    private Handle h;

    @BeforeEach
    public void startUp() {
        Assertions.setMaxStackTraceElementsDisplayed(100);
        this.h = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    public void testAfterCommitThrowsRollback() {
        assertThatThrownBy(() -> h.useTransaction(inner -> {
            inner.execute("create table names(id int, name varchar)");
            h.afterCommit(() ->
                h.useTransaction(inner2 -> {
                    assertThat(h.createQuery("select name from names where id = 1").mapTo(String.class).one())
                            .isEqualTo("Kafka");
                    throw new IllegalStateException("boom");
                }));
            inner.execute("insert into names (id, name) values (1, 'Kafka')");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom")
                .hasNoSuppressedExceptions();
        assertThat(h.createQuery("select name from names where id = 1").mapTo(String.class).one())
                .isEqualTo("Kafka");
    }
}
