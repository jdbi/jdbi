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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.subpackage.PrivateImplementationFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBeanBinder {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
    }

    @Test
    public void testInsert() {
        Spiffy s = handle.attach(Spiffy.class);
        s.insert(new Something(2, "Bean"));

        String name = handle.createQuery("select name from something where id = 2").mapTo(String.class).findOnly();
        assertThat(name).isEqualTo("Bean");
    }

    @Test
    public void testRead() {
        Spiffy s = handle.attach(Spiffy.class);
        handle.execute("insert into something (id, name) values (17, 'Phil')");
        Something phil = s.findByEqualsOnBothFields(new Something(17, "Phil"));
        assertThat(phil.getName()).isEqualTo("Phil");
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Spiffy {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = :s.id and name = :s.name")
        Something findByEqualsOnBothFields(@BindBean("s") Something s);

        @SqlQuery("select :pi.value")
        String selectPublicInterfaceValue(@BindBean("pi") PublicInterface pi);
    }

    @Test
    public void testBindingPrivateTypeUsingPublicInterface() {
        Spiffy s = handle.attach(Spiffy.class);
        assertThat(s.selectPublicInterfaceValue(PrivateImplementationFactory.create())).isEqualTo("IShouldBind");
    }

    public interface PublicInterface {
        String getValue();
    }
}
