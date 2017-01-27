package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SqlObjectTest
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    // tag::defn[]
    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao
    {
        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        Optional<Something> findSomething(int id);
    }
    // end::defn[]

    SomethingDao dao;

    @Before
    public void setUp()
    {
        final Handle handle = dbRule.getSharedHandle();
        dao = handle.attach(SomethingDao.class);
    }

    @Test
    public void testFindById() throws Exception
    {
        // tag::find-by-id[]
        dao.insert(new Something(1, "apple"));
        dao.insert(new Something(2, "bison"));

        assertThat(dao.findSomething(2))
            .isPresent()
            .contains(new Something(2, "bison"));

        assertThat(dao.findSomething(3))
            .isEmpty();
        // end::find-by-id[]
    }
}
