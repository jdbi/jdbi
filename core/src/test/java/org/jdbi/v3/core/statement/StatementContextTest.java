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
package org.jdbi.v3.core.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StatementContextTest {

    @Test
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() throws Exception {
        final StatementContext context = StatementContextAccess.createContext();

        context.setReturningGeneratedKeys(true);
        assertThatThrownBy(() -> context.setConcurrentUpdatable(true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() throws Exception {
        final StatementContext context = StatementContextAccess.createContext();

        context.setConcurrentUpdatable(true);
        assertThatThrownBy(() -> context.setReturningGeneratedKeys(true)).isInstanceOf(IllegalArgumentException.class);
    }

    private static class Foo {}

    private static class FooMapper implements ColumnMapper<Foo> {
        @Override
        public Foo map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    @Test
    public void testMapperForDelegatesToRegistry() {
        ColumnMapper<Foo> mapper = new FooMapper();

        ConfigRegistry config = new ConfigRegistry();
        config.get(ColumnMappers.class).register(mapper);

        final StatementContext context = StatementContextAccess.createContext(config);

        assertThat(context.findColumnMapperFor(Foo.class)).contains(mapper);
    }
}
