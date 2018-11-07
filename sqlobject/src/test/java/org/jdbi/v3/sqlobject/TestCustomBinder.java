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
package org.jdbi.v3.sqlobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

public class TestCustomBinder {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testFoo() {
        dbRule.getSharedHandle().execute("insert into something (id, name) values (2, 'Martin')");
        dbRule.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Something s = spiffy.findSame(new Something(2, "Unknown"));
            assertThat(s.getName()).isEqualTo("Martin");
        });
    }

    @Test
    public void testCustomBindingAnnotation() {
        Spiffy s = dbRule.getSharedHandle().attach(Spiffy.class);

        s.insert(new Something(2, "Keith"));

        assertThat(s.findNameById(2)).isEqualTo("Keith");
    }

    @Test
    public void testBar() {
        dbRule.getSharedHandle().execute("insert into something (id, name) values (2, 'Martin')");
        dbRule.getJdbi().useExtension(Spiffier.class, spiffier -> {
            Something s = spiffier.findSame(new Something(2, "Unknown"));
            assertThat(s.getName()).isEqualTo("Martin");
        });
    }

    @Test
    public void testCustomBindingType() {
        Spiffier s = dbRule.getSharedHandle().attach(Spiffier.class);

        s.insert(new Something(23, "Frank"));

        assertThat(s.findNameById(23)).isEqualTo("Frank");
    }

    @Test
    public void testBaz() {
        dbRule.getSharedHandle().execute("insert into something (id, name) values (2, 'Martin')");
        dbRule.getJdbi().useExtension(Spiffiest.class, spiffiest -> {
            try {
                Something s = spiffiest.findSame(new Something(2, "Unknown"));
                fail("Should have thrown ClassCastException");
            } catch (ClassCastException cce) {
                assertThat(cce.getMessage()).isEqualTo("org.jdbi.v3.core.Something cannot be cast to java.lang.Integer");
            }
        });
    }

    @Test
    public void testCustomBindingWrongType() {
        Spiffiest s = dbRule.getSharedHandle().attach(Spiffiest.class);

        try {
            s.insert(new Something(23, "Frank"));
            fail("Should have thrown ClassCastException");
        } catch (ClassCastException cce) {
            assertThat(cce.getMessage()).isEqualTo("org.jdbi.v3.core.Something cannot be cast to java.lang.Integer");
        }
    }

    public interface Spiffy {
        @SqlQuery("select id, name from something where id = :it.id")
        @UseRowMapper(SomethingMapper.class)
        Something findSame(@BindSomething("it") Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        int insert(@BindSomething("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }

    public interface Spiffier {
        @SqlQuery("select id, name from something where id = :it.id")
        @UseRowMapper(SomethingMapper.class)
        Something findSame(@BindSomethingElse("it") Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        int insert(@BindSomethingElse("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }

    public interface Spiffiest {
        @SqlQuery("select id, name from something where id = :it.id")
        @UseRowMapper(SomethingMapper.class)
        Something findSame(@BindSomethingOther("it") Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        int insert(@BindSomethingOther("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }
}
