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
package org.jdbi.core.statement;

import java.sql.ResultSet;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.qualifier.NVarchar;
import org.jdbi.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StatementContextTest {
    @Test
    public void qualifiedTypeShouldBePersistent() {
        StatementContext context = StatementContextAccess.createContext();

        // it's about the return type being String and not ?
        Optional<ColumnMapper<String>> mapper = context.findColumnMapperFor(QualifiedType.of(String.class).with(NVarchar.class));
        assertThat(mapper).isPresent();
    }

    @Test
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() {
        final StatementContext context = StatementContextAccess.createContext();

        context.setReturningGeneratedKeys(true);
        assertThatThrownBy(() -> context.setConcurrentUpdatable(true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() {
        final StatementContext context = StatementContextAccess.createContext();

        context.setConcurrentUpdatable(true);
        assertThatThrownBy(() -> context.setReturningGeneratedKeys(true)).isInstanceOf(IllegalArgumentException.class);
    }

    private static class Foo {}

    private static class FooMapper implements ColumnMapper<Foo> {
        @Override
        public Foo map(ResultSet r, int columnNumber, StatementContext ctx) {
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
