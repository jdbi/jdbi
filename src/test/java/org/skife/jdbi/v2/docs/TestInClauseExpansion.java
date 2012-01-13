package org.skife.jdbi.v2.docs;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterArgumentFactory;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestInClauseExpansion
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:test");

        dbi.setSQLLog(new PrintStreamLog(System.out));
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

        DAO dao = handle.attach(DAO.class);

        assertThat(dao.findIdsForNames(asList(1, 2)), equalTo(ImmutableSet.of("Brian", "Jeff")));
    }

    @ExternalizedSqlViaStringTemplate3
    @RegisterContainerMapper(ImmutableSetContainerFactory.class)
    public static interface DAO
    {
        @SqlQuery
        public ImmutableSet<String> findIdsForNames(@BindIn("names") List<Integer> names);
    }

    public static class ImmutableSetContainerFactory implements ContainerFactory<ImmutableSet>
    {

        public boolean accepts(Class<?> type)
        {
            return ImmutableSet.class.isAssignableFrom(type);
        }

        public ContainerBuilder<ImmutableSet> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<ImmutableSet>()
            {
                final ImmutableSet.Builder<Object> builder = ImmutableSet.builder();

                public ContainerBuilder<ImmutableSet> add(Object it)
                {
                    builder.add(it);
                    return this;
                }

                public ImmutableSet build()
                {
                    return builder.build();
                }
            };
        }
    }
}
