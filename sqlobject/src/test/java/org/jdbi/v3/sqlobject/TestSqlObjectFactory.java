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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestSqlObjectFactory {

    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        jdbi = Jdbi.create(() -> {
            throw new UnsupportedOperationException();
        });

        jdbi.registerExtension(new SqlObjectFactory());
    }

    @Test
    public void accepts() {
        ExtensionFactory factory = jdbi.getConfig(Extensions.class).findFactory(SqlObjectFactory.class)
                .orElseGet(() -> fail("Could not retrieve factory"));

        assertThat(factory.accepts(NotASqlObject.class)).isFalse();

        assertThat(factory.accepts(SqlObjectClass.class)).isFalse();

        assertThat(factory.accepts(HasAnnotatedMethod.class)).isTrue();
        assertThat(factory.accepts(ExtendsSqlObject.class)).isTrue();
    }

    abstract static class NotASqlObject {

        abstract String foo(String id);
    }

    interface NonPublicSqlObject extends SqlObject {}

    public abstract static class SqlObjectClass implements SqlObject {}

    public interface HasAnnotatedMethod {

        @SqlQuery("select foo from bar")
        String foo();
    }

    public interface ExtendsSqlObject extends SqlObject {}
}
