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
package org.jdbi.v3.noparameters;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.customizer.DefineList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeFalse;

public class TestSqlObjectNoParameterNames {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle h;

    @Before
    public void setUp() throws Exception {
        assumeFalse(BindDao.class.getMethod("getByIdImplicitBindPositional", int.class).getParameters()[0].isNamePresent());

        h = dbRule.getSharedHandle();
        h.registerRowMapper(BeanMapper.factory(Something.class));

        h.execute("insert into something (id, name) values (1, 'Elsie Hughes')");
    }

    @Test
    public void implicitBindPositional() {
        assertThat(h.attach(BindDao.class).getByIdImplicitBindPositional(1)).isEqualTo(new Something(1, "Elsie Hughes"));
    }

    @Test
    public void explicitBindPositional() {
        assertThat(h.attach(BindDao.class).getByIdExplicitBindPositional(1)).isEqualTo(new Something(1, "Elsie Hughes"));
    }

    @Test
    public void implicitBindNamed() {
        assertThatThrownBy(() -> h.attach(BindDao.class).getByIdImplicitBindNamed(1))
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("Missing named parameter 'id'");
    }

    @Test
    public void explicitBindNamed() {
        assertThatThrownBy(() -> h.attach(BindDao.class).getByIdExplicitBindNamed(1))
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("Missing named parameter 'id'");
    }

    public interface BindDao {
        @SqlQuery("select * from something where id = ?")
        Something getByIdImplicitBindPositional(int id);

        @SqlQuery("select * from something where id = ?")
        Something getByIdExplicitBindPositional(@Bind int id);

        @SqlQuery("select * from something where id = :id")
        Something getByIdImplicitBindNamed(@Bind int id);

        @SqlQuery("select * from something where id = :id")
        Something getByIdExplicitBindNamed(@Bind int id);
    }

    @Test
    public void bindListMissingName() {
        assertThatThrownBy(() -> h.attach(BindListWithoutNameDao.class).listByIds(1))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("parameter was not given a name");
    }

    public interface BindListWithoutNameDao {
        @SqlQuery("select * from something where id in (<ids>)")
        List<Something> listByIds(@BindList int... ids);
    }

    @Test
    public void bindListWithName() {
        assertThat(h.attach(BindListWithNameDao.class).listByIds(1))
                .contains(new Something(1, "Elsie Hughes"));
    }

    public interface BindListWithNameDao {
        @SqlQuery("select * from something where id in (<ids>)")
        List<Something> listByIds(@BindList("ids") int... ids);
    }

    @Test
    public void defineMissingName() {
        assertThatThrownBy(() -> h.attach(DefineWithoutNameDao.class).getById(1))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("parameter was not given a name");
    }

    public interface DefineWithoutNameDao {
        // DON'T DO THIS IN PRODUCTION CODE, IT'S JUST A LAZY TEST
        @SqlQuery("select * from something where id = <id>")
        Something getById(@Define int id);
    }

    @Test
    public void defineWithName() {
        assertThat(h.attach(DefineWithNameDao.class).getById(1))
                .isEqualTo(new Something(1, "Elsie Hughes"));
    }

    public interface DefineWithNameDao {
        // DON'T DO THIS IN PRODUCTION CODE, IT'S JUST A LAZY TEST
        @SqlQuery("select * from something where id = <id>")
        Something getById(@Define("id") int id);
    }

    @Test
    public void defineListWithoutName() {
        assertThatThrownBy(() -> h.attach(DefineListWithoutNameDao.class).listByIds("1"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("parameter was not given a name");
    }

    public interface DefineListWithoutNameDao {
        // DON'T DO THIS IN PRODUCTION CODE, IT'S JUST A LAZY TEST
        @SqlQuery("select * from something where id in (<ids>)")
        List<Something> listByIds(@DefineList String... ids);
    }

    @Test
    public void defineListWithName() {
        assertThat(h.attach(DefineListWithNameDao.class).listByIds("1"))
                .containsExactly(new Something(1, "Elsie Hughes"));
    }

    public interface DefineListWithNameDao {
        // DON'T DO THIS IN PRODUCTION CODE, IT'S JUST A LAZY TEST
        @SqlQuery("select * from something where id in (<ids>)")
        List<Something> listByIds(@DefineList("ids") String... ids);
    }
}
