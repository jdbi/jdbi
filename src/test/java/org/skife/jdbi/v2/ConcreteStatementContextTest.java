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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConcreteStatementContextTest {


    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() throws Exception {
        final ConcreteStatementContext context = new ConcreteStatementContext();

        context.setReturningGeneratedKeys(true);
        context.setConcurrentUpdatable(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() throws Exception {
        final ConcreteStatementContext context = new ConcreteStatementContext();

        context.setConcurrentUpdatable(true);
        context.setReturningGeneratedKeys(true);
    }

    private static class Foo {
    }

    private static class FooMapper implements ResultColumnMapper<Foo> {
        @Override
        public Foo mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return null;
        }

        @Override
        public Foo mapColumn(ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    @Test
    public void testMapperForDelegatesToRegistry() {
        ResultColumnMapper mapper = new FooMapper();

        MappingRegistry registry = new MappingRegistry();
        registry.addColumnMapper(mapper);

        final ConcreteStatementContext context =
                new ConcreteStatementContext(Collections.<String, Object>emptyMap(), registry, new SqlObjectContext());

        assertThat(context.columnMapperFor(Foo.class), equalTo(mapper));
    }
}
