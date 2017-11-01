package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath;

import java.sql.Types;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.OutParameters;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.junit.Rule;
import org.junit.Test;

public class CallTest
{
    @Rule
    public PostgresDbRule db = new PostgresDbRule();

    @Test
    public void testCall() {
        Handle handle = db.getSharedHandle();

        handle.execute(findSqlOnClasspath("create_stored_proc_add"));

        // tag::invokeProcedure[]
        OutParameters result = handle
                .createCall("{:sum = add(:a, :b)}") // <1>
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
