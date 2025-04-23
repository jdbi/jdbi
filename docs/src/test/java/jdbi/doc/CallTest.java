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
package jdbi.doc;

import java.sql.Types;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.OutParameters;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CallTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg);

    @Test
    public void testCall() {
        Handle handle = pgExtension.getSharedHandle();

        handle.execute(ClasspathSqlLocator.removingComments().locate("create_stored_proc_add"));

        // tag::invokeProcedure[]
        try (Call call = handle.createCall("{:sum = call add(:a, :b)}")) { // <1>
            OutParameters result = call.bind("a", 13) // <2>
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
}
