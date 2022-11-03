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
package org.jdbi.v3.core.statement;

import java.sql.Types;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.NoResultsException;
import org.jdbi.v3.core.result.ResultProducers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestStatements {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testStatement() {
        Handle h = h2Extension.getSharedHandle();

        int rows = h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        assertThat(rows).isOne();
    }

    @Test
    public void testSimpleInsert() {
        Handle h = h2Extension.getSharedHandle();

        int c = h.execute("insert into something (id, name) values (1, 'eric')");
        assertThat(c).isOne();
    }

    @Test
    public void testUpdate() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.createUpdate("update something set name = 'ERIC' where id = 1").execute();
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("ERIC");
    }

    @Test
    public void testSimpleUpdate() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("update something set name = 'cire' where id = 1");
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("cire");
    }

    @Test
    public void testStatementWithRequiredResults() {
        Handle h = h2Extension.getSharedHandle();

        assertThatThrownBy(() -> {
            try (Query query = h.createQuery("commit")) {
                query.mapTo(Integer.class).findFirst();
            }
        }).isInstanceOf(NoResultsException.class);
    }

    @Test
    public void testStatementWithOptionalResults() {
        Handle h = h2Extension.getSharedHandle();

        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapTo(Integer.class).findFirst()).isEmpty();
    }

    @Test
    public void testStatementWithOptionalBeanResults() {
        Handle h = h2Extension.getSharedHandle();

        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapToBean(Object.class).findFirst()).isEmpty();
    }

    @Test
    public void testStatementWithOptionalMapResults() {
        Handle h = h2Extension.getSharedHandle();

        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapToMap().findFirst()).isEmpty();
    }

    @Test
    public void testUnusedBinding() {
        Handle h = h2Extension.getSharedHandle();

        assertThatThrownBy(() -> h.createQuery("select * from something")
            .bind("id", 1)
            .collectRows(Collectors.counting())
        ).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPermittedUnusedBinding() {
        Handle h = h2Extension.getSharedHandle();

        assertThatCode(() -> h.configure(SqlStatements.class, s -> s.setUnusedBindingAllowed(true))
            .createQuery("select * from something")
            .bind("id", 1)
            .collectRows(Collectors.counting())).doesNotThrowAnyException();
    }

    @Test
    public void testPermittedUsedAndUnusedBinding() {
        Handle h = h2Extension.getSharedHandle();

        assertThatCode(() -> h.configure(SqlStatements.class, s -> s.setUnusedBindingAllowed(true))
            .createQuery("select * from something where id = :id")
            .bind("id", 1)
            .bind("name", "jack")
            .collectRows(Collectors.counting())).doesNotThrowAnyException();
    }

    @Test
    // TODO it would be nice if this failed in the future
    public void testUsedAndUnusedNamed() {
        Handle h = h2Extension.getSharedHandle();

        assertThatCode(() -> h.createQuery("select * from something where id = :id")
            .bind("id", 1)
            .bind("name", "jack")
            .collectRows(Collectors.counting())
        ).doesNotThrowAnyException();
    }

    @Test
    public void testFarAwayPositional() {
        Handle h = h2Extension.getSharedHandle();

        assertThatThrownBy(() -> h.createQuery("select * from something where id = ?")
            .bind(0, 1)
            .bind(2, "jack")
            .collectRows(Collectors.counting())
        ).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testUnusedBindingWithOutParameter() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("CREATE ALIAS TO_DEGREES FOR \"java.lang.Math.toDegrees\"");

        Call call = h.createCall("? = CALL TO_DEGREES(?)")
            .registerOutParameter(0, Types.DOUBLE)
            .bind(1, 100.0d)
            .bind(2, "foo");

        assertThatThrownBy(call::invoke).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPermittedUnusedBindingWithOutParameter() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("CREATE ALIAS TO_DEGREES FOR \"java.lang.Math.toDegrees\"");

        Call call = h.configure(SqlStatements.class, stmts -> stmts.setUnusedBindingAllowed(true))
            .createCall("? = CALL TO_DEGREES(?)")
            .registerOutParameter(0, Types.DOUBLE)
            .bind(1, 100.0d)
            .bind(2, "foo");

        assertThatCode(call::invoke).doesNotThrowAnyException();
    }
}
