package jdbi.doc;

import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlObjectTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething().withPlugin(new SqlObjectPlugin());

    // tag::defn[]
    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao {

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        Optional<Something> findSomething(int id);
    }
    // end::defn[]

    SomethingDao dao;

    @BeforeEach
    public void setUp() {
        final Handle handle = h2Extension.getSharedHandle();
        dao = handle.attach(SomethingDao.class);
    }

    @Test
    public void testFindById() {
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
