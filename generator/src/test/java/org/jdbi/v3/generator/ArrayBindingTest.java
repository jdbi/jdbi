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
package org.jdbi.v3.generator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayBindingTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugins(new H2DatabasePlugin(), new SqlObjectPlugin())
        .withConfig(Extensions.class, c -> c.setAllowProxy(false));

    private Handle handle;
    private BazDao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table baz (baz integer array)");
        dao = handle.attach(BazDao.class);
    }

    @Test
    public void simpleGeneratedClass() {
        final Baz baz1 = new Baz(1, 2, 3);
        dao.insert(baz1);
        final Baz baz2 = new Baz(2, 3, 4);
        dao.insert(baz2);

        assertThat(dao.list())
                .containsExactlyInAnyOrder(baz1, baz2);
    }

    @Test
    public void testForHandle() {
        ((BazDaoImpl) dao).withHandle(h -> assertThat(h).isNotNull());
    }

    @GenerateSqlObject
    @RegisterConstructorMapper(Baz.class)
    interface BazDao {
        @SqlUpdate("insert into baz (baz) values (:baz)")
        void insert(@BindFields Baz value);

        @SqlQuery("select baz from baz")
        List<Baz> list();
    }

    public static class Baz {
        public Baz(final int... baz) {
            this.baz = IntStream.of(baz)
                    .boxed()
                    .collect(Collectors.toList());
        }

        public final List<Integer> baz;

        @Override
        public int hashCode() {
            return baz.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Baz other && Objects.equals(other.baz, baz);
        }
    }
}
