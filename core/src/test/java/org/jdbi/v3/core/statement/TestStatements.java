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
import org.jdbi.v3.core.result.NoResultsException;
import org.jdbi.v3.core.result.ResultProducers;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestStatements {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp() throws Exception {
        h = dbRule.openHandle();
    }

    @After
    public void doTearDown() throws Exception {
        if (h != null) {
            h.close();
        }
    }

    @Test
    public void testStatement() {
        int rows = h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        assertThat(rows).isEqualTo(1);
    }

    @Test
    public void testSimpleInsert() {
        int c = h.execute("insert into something (id, name) values (1, 'eric')");
        assertThat(c).isEqualTo(1);
    }

    @Test
    public void testUpdate() {
        h.execute("insert into something (id, name) values (1, 'eric')");
        h.createUpdate("update something set name = 'ERIC' where id = 1").execute();
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("ERIC");
    }

    @Test
    public void testSimpleUpdate() {
        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("update something set name = 'cire' where id = 1");
        Something eric = h.createQuery("select * from something where id = 1").mapToBean(Something.class).list().get(0);
        assertThat(eric.getName()).isEqualTo("cire");
    }

    @Test
    public void testStatementWithRequiredResults() {
        assertThatThrownBy(() -> h.createQuery("commit").mapTo(Integer.class).findFirst()).isInstanceOf(NoResultsException.class);
    }

    @Test
    public void testStatementWithOptionalResults() {
        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapTo(Integer.class).findFirst()).isEmpty();
    }

    @Test
    public void testStatementWithOptionalBeanResults() {
        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapToBean(Object.class).findFirst()).isEmpty();
    }

    @Test
    public void testStatementWithOptionalMapResults() {
        h.getConfig(ResultProducers.class).allowNoResults(true);
        assertThat(h.createQuery("commit").mapToMap().findFirst()).isEmpty();
    }

    @Test
    public void testUnusedBinding() {
        assertThatThrownBy(() -> h.createQuery("select * from something")
            .bind("id", 1)
            .collectRows(Collectors.counting())
        ).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPermittedUnusedBinding() {
       assertThatCode(() -> h.configure(SqlStatements.class, s -> s.setUnusedBindingAllowed(true))
           .createQuery("select * from something")
           .bind("id", 1)
           .collectRows(Collectors.counting())).doesNotThrowAnyException();
    }

    @Test
    public void testPermittedUsedAndUnusedBinding() {
        assertThatCode(() -> h.configure(SqlStatements.class, s -> s.setUnusedBindingAllowed(true))
            .createQuery("select * from something where id = :id")
            .bind("id", 1)
            .bind("name", "jack")
            .collectRows(Collectors.counting())).doesNotThrowAnyException();
    }

    @Test
    // TODO it would be nice if this failed in the future
    public void testUsedAndUnusedNamed() {
        assertThatCode(() -> h.createQuery("select * from something where id = :id")
            .bind("id", 1)
            .bind("name", "jack")
            .collectRows(Collectors.counting())
        ).doesNotThrowAnyException();
    }

    @Test
    // TODO it would be nice if this failed in the future
    public void testFarAwayPositional() {
        assertThatCode(() -> h.createQuery("select * from something where id = ?")
            .bind(0, 1)
            .bind(2, "jack")
            .collectRows(Collectors.counting())
        ).doesNotThrowAnyException();
    }

    @Test
    public void testUnusedBindingWithOutParameter() {
        h.execute("CREATE ALIAS TO_DEGREES FOR \"java.lang.Math.toDegrees\"");

        Call call = h.createCall("? = CALL TO_DEGREES(?)")
            .registerOutParameter(0, Types.DOUBLE)
            .bind(1, 100.0d)
            .bind(2, "foo");

        assertThatThrownBy(call::invoke).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testPermittedUnusedBindingWithOutParameter() {
        h.execute("CREATE ALIAS TO_DEGREES FOR \"java.lang.Math.toDegrees\"");

        Call call = h.configure(SqlStatements.class, stmts -> stmts.setUnusedBindingAllowed(true))
            .createCall("? = CALL TO_DEGREES(?)")
            .registerOutParameter(0, Types.DOUBLE)
            .bind(1, 100.0d)
            .bind(2, "foo");

        assertThatCode(call::invoke).doesNotThrowAnyException();
    }
}
