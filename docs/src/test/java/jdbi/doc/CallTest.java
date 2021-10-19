package jdbi.doc;

import java.sql.Types;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.jdbi.v3.core.statement.OutParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath;

public class CallTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    @Test
    public void testCall() {
        Handle handle = pgExtension.openHandle();

        handle.execute(findSqlOnClasspath("create_stored_proc_add"));

        // tag::invokeProcedure[]
        OutParameters result = handle
            .createCall("{:sum = call add(:a, :b)}") // <1>
                .bind("a", 13) // <2>
                .bind("b", 9) // <2>
                .registerOutParameter("sum", Types.INTEGER) // <3> <4>
                .invoke(); // <5>
        // end::invokeProcedure[]

        // tag::getOutParameters[]
        int sum = result.getInt("sum");
        // end::getOutParameters[]

        assertThat(sum).isEqualTo(22);
    }
}
