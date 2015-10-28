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
package org.jdbi.v3.docs;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.RegisterCollectorFactory;
import org.jdbi.v3.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.jdbi.v3.tweak.CollectorFactory;
import org.jdbi.v3.unstable.BindIn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class TestInClauseExpansion
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testInClauseExpansion() throws Exception
    {
        handle.execute("insert into something (name, id) values ('Brian', 1), ('Jeff', 2), ('Tom', 3)");

        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);

        assertEquals(ImmutableSet.of("Brian", "Jeff"), dao.findIdsForNames(asList(1, 2)));
    }

    @UseStringTemplate3StatementLocator
    @RegisterCollectorFactory(ImmutableSetCollectorFactory.class)
    public interface DAO
    {
        @SqlQuery
        ImmutableSet<String> findIdsForNames(@BindIn("names") List<Integer> names);
    }

    public static class ImmutableSetCollectorFactory<T> implements CollectorFactory<T, ImmutableSet<T>> {

        public boolean accepts(Class<?> type) {
            return ImmutableSet.class.isAssignableFrom(type);
        }

        @Override
        public Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> newCollector(Class<ImmutableSet<T>> type) {
            return Collector.of(ImmutableSet.Builder::new, ImmutableSet.Builder::add, (first, second) -> {
                throw new UnsupportedOperationException("Parallel collecting is not supported");
            }, ImmutableSet.Builder::build, Collector.Characteristics.UNORDERED);
        }
    }
}
