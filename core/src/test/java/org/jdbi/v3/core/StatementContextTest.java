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
package org.jdbi.v3.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.junit.Test;

public class StatementContextTest {


    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineGeneratedKeysAndConcurrentUpdatable() throws Exception {
        final StatementContext context = new StatementContext();

        context.setReturningGeneratedKeys(true);
        context.setConcurrentUpdatable(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldNotBeAbleToCombineConcurrentUpdatableAndGeneratedKeys() throws Exception {
        final StatementContext context = new StatementContext();

        context.setConcurrentUpdatable(true);
        context.setReturningGeneratedKeys(true);
    }

    private static class Foo {
    }

    private static class FooMapper implements ColumnMapper<Foo> {
        @Override
        public Foo map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    @Test
    public void testMapperForDelegatesToRegistry() {
        ColumnMapper<?> mapper = new FooMapper();

        JdbiConfig config = new JdbiConfig();
        config.mappingRegistry.addColumnMapper(mapper);

        final StatementContext context = new StatementContext(config);

        assertThat(context.findColumnMapperFor(Foo.class).get(), equalTo(mapper));
    }
}
