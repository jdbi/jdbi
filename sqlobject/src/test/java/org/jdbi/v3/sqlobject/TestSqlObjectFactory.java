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

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestSqlObjectFactory {
    private SqlObjectFactory factory = new SqlObjectFactory();

    @Test
    public void accepts() {
        assertThat(factory.accepts(NotASqlObject.class)).isFalse();

        assertThatThrownBy(() -> factory.accepts(NonPublicSqlObject.class))
                .hasMessageContaining("must be public");

        assertThatThrownBy(() -> factory.accepts(SqlObjectClass.class))
                .hasMessageContaining("only supported for interfaces");

        assertThat(factory.accepts(HasAnnotatedMethod.class)).isTrue();
        assertThat(factory.accepts(ExtendsSqlObject.class)).isTrue();
    }

    abstract class NotASqlObject {
        abstract String foo(String id);
    }

    interface NonPublicSqlObject extends SqlObject {
    }

    public abstract class SqlObjectClass implements SqlObject {
    }

    public interface HasAnnotatedMethod {
        @SqlQuery("select foo from bar")
        String foo();
    }

    public interface ExtendsSqlObject extends SqlObject {
    }
}
