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
package org.jdbi.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.core.Handle;
import org.jdbi.core.statement.Customizable;
import org.jdbi.sqlobject.TestConfigMutatingCustomizer.DefineViaConfig;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @SqlUpdate}, {@code @SqlCall}, and {@code @SqlBatch} adopt a reusable {@link
 * org.jdbi.core.statement.StatementTemplate} the same way {@code @SqlQuery} does: one template is built per
 * attach, its configure-phase customizers are baked into the configuration snapshot once, and every
 * invocation binds a fresh, thread-confined statement against it. A method with a configuration-mutating
 * customizer ({@link org.jdbi.sqlobject.customizer.ConfigMutating}) instead runs on the classic per-invocation
 * path so the mutation takes effect and does not leak between calls.
 */
public class TestSqlObjectTemplateAdoption {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    // Counts how many times a configure-phase customizer is applied; a template bakes it in once per attach.
    private static final AtomicInteger CONFIGURE_APPLICATIONS = new AtomicInteger();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        CONFIGURE_APPLICATIONS.set(0);
        handle = h2Extension.getSharedHandle();
        handle.execute("create table something (id int primary key, name varchar(50))");
        handle.execute("insert into something (id, name) values (1, 'Alice')");
    }

    @Test
    public void updateBakesConfigureCustomizerOncePerAttach() {
        UpdateDao dao = handle.attach(UpdateDao.class);

        assertThat(dao.rename(1, "Bob")).isOne();
        assertThat(dao.rename(1, "Carol")).isOne();
        assertThat(dao.rename(1, "Dave")).isOne();

        // The template is built once on first use and reused; the configure-phase customizer ran once.
        assertThat(CONFIGURE_APPLICATIONS).hasValue(1);
        assertThat(handle.createQuery("select name from something where id = 1").mapTo(String.class).one())
                .isEqualTo("Dave");
    }

    @Test
    public void updateConfigMutatingCustomizerRunsClassicPathPerInvocation() {
        MutatingUpdateDao dao = handle.attach(MutatingUpdateDao.class);

        // Each call renders a different WHERE column through per-invocation configuration mutation.
        assertThat(dao.renameWhere("Bob", "id", 1)).isOne();
        assertThat(dao.renameWhere("Carol", "name", "Bob")).isOne();

        assertThat(handle.createQuery("select name from something where id = 1").mapTo(String.class).one())
                .isEqualTo("Carol");
    }

    @Test
    public void batchBakesConfigureCustomizerOncePerAttach() {
        BatchDao dao = handle.attach(BatchDao.class);

        dao.insert(List.of(2, 3), List.of("Bob", "Carol"));
        dao.insert(List.of(4, 5), List.of("Dave", "Eve"));

        assertThat(CONFIGURE_APPLICATIONS).hasValue(1);
        assertThat(handle.createQuery("select count(*) from something").mapTo(int.class).one()).isEqualTo(5);
    }

    @Test
    public void batchConfigMutatingCustomizerRunsClassicPath() {
        MutatingBatchDao dao = handle.attach(MutatingBatchDao.class);

        dao.insert(List.of(2, 3), List.of("Bob", "Carol"), "unused");

        assertThat(handle.createQuery("select count(*) from something").mapTo(int.class).one()).isEqualTo(3);
    }

    public interface UpdateDao {
        @CountConfigure
        @SqlUpdate("update something set name = :name where id = :id")
        int rename(@Bind("id") int id, @Bind("name") String name);
    }

    public interface MutatingUpdateDao {
        @SqlUpdate("update something set name = :name where <whereCol> = :whereVal")
        int renameWhere(@Bind("name") String name, @DefineViaConfig("whereCol") String whereCol, @Bind("whereVal") Object whereVal);
    }

    public interface BatchDao {
        @CountConfigure
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") List<Integer> ids, @Bind("name") List<String> names);
    }

    public interface MutatingBatchDao {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") List<Integer> ids, @Bind("name") List<String> names, @DefineViaConfig("unused") String unused);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @SqlStatementCustomizingAnnotation(CountConfigure.Factory.class)
    public @interface CountConfigure {
        class Factory implements SqlStatementCustomizerFactory {
            @Override
            public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
                return TestSqlObjectTemplateAdoption::countConfigureApplication;
            }
        }
    }

    // A configure-phase customizer that only records that it ran, so the test can prove it is baked once.
    private static void countConfigureApplication(Customizable<?> stmt) {
        CONFIGURE_APPLICATIONS.incrementAndGet();
    }
}
