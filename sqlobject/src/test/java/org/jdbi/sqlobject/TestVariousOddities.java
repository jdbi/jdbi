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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.testing.EqualsTester;
import org.jdbi.core.Handle;
import org.jdbi.core.HandleCallback;
import org.jdbi.core.HandleConsumer;
import org.jdbi.core.JdbiException;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.transaction.Transactional;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestVariousOddities {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    @Test
    public void testAttach() {
        Spiffy s = h2Extension.getSharedHandle().attach(Spiffy.class);
        s.insert(new Something(14, "Tom"));

        Something tom = s.byId(14);
        assertThat(tom.getName()).isEqualTo("Tom");
    }

    @Test
    public void testEquals() {
        Spiffy s1 = h2Extension.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = h2Extension.getSharedHandle().attach(Spiffy.class);
        assertThat(s1).isNotSameAs(s2);
        new EqualsTester()
            .addEqualityGroup(s1, s1)
            .addEqualityGroup(s2)
            .testEquals();
    }

    @Test
    public void testToString() {
        Spiffy s1 = h2Extension.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = h2Extension.getSharedHandle().attach(Spiffy.class);
        assertThat(s1.toString()).isNotNull();
        assertThat(s2.toString()).isNotNull();
        assertThat(s1.toString()).isNotEqualTo(s2.toString());
    }

    @Test
    public void testHashCode() {
        Spiffy s1 = h2Extension.getSharedHandle().attach(Spiffy.class);
        Spiffy s2 = h2Extension.getSharedHandle().attach(Spiffy.class);
        assertThat(s1.hashCode()).isNotZero();
        assertThat(s2.hashCode()).isNotZero();
        assertThat(s1.hashCode()).isNotEqualTo(s2.hashCode());
    }

    @Test
    public void testConcurrentHashCode() throws ExecutionException, InterruptedException {
        Callable<SpiffyConcurrent> callable = () ->
            h2Extension.getSharedHandle().attach(SpiffyConcurrent.class);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<SpiffyConcurrent> f1 = pool.submit(callable);
        Future<SpiffyConcurrent> f2 = pool.submit(callable);
        pool.shutdown();
        SpiffyConcurrent s1 = f1.get();
        SpiffyConcurrent s2 = f2.get();

        assertThat(s1.hashCode()).isNotZero();
        assertThat(s2.hashCode()).isNotZero();
        assertThat(s1.hashCode()).isNotEqualTo(s2.hashCode());
    }

    @Test
    public void testNullQueryReturn() {
        Handle h = h2Extension.getSharedHandle();
        assertThatThrownBy(() -> h.attach(SpiffyBoom.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("returnNothing is annotated as if it should return a value, but the method is void.");
    }

    public interface Spiffy {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@BindSomething("it") Something it);

    }

    public interface SpiffyBoom {
        @SqlQuery("SELECT 1")
        void returnNothing();
    }

    /**
     * This interface should not be loaded by any test other than {@link TestVariousOddities#testConcurrentHashCode()}.
     */
    public interface SpiffyConcurrent extends SqlObject {}

    @Test
    public void testInterfaceAmbiguousMethods() {
        assertThatThrownBy(() ->
            h2Extension.getSharedHandle().attach(AmbiguousMethods.class))
            .hasMessageContaining("AmbiguousMethods has ambiguous methods")
            .isInstanceOf(JdbiException.class);
    }

    @Test
    public void testAmbiguityResolved() {
        ResolvedMethods methods = h2Extension.getSharedHandle().attach(ResolvedMethods.class);
        assertThat(methods.value()).isEqualTo("resolved");
    }

    public interface VersionA {
        @SqlQuery("select 'intriguing'")
        String value();
    }
    public interface VersionB {
        @SqlQuery("select 'indubitably'")
        String value();
    }

    public interface AmbiguousMethods extends VersionA, VersionB {}
    public interface ResolvedMethods extends AmbiguousMethods {
        @SqlQuery("select 'resolved'")
        @Override
        String value();
    }

    public interface OnDemandOddities extends SqlObject, Transactional<OnDemandOddities> {
        @CreateSqlObject
        VersionA versionA();
    }

    public class DecoratedOnDemandOddities implements OnDemandOddities {

        private OnDemandOddities onDemand = h2Extension.getJdbi().onDemand(OnDemandOddities.class);

        @Override
        public Handle getHandle() {
            return onDemand.getHandle();
        }

        @Override
        public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
            return onDemand.withHandle(callback);
        }

        @Override
        public <X extends Exception> void useHandle(HandleConsumer<X> consumer) throws X {
            onDemand.useHandle(consumer);
        }

        @Override
        public VersionA versionA() {
            return onDemand.versionA();
        }
    }

    @Test
    public void onDemandCreateSqlObject() throws Exception {
        assertThat(h2Extension.getJdbi().onDemand(OnDemandOddities.class).versionA().value())
                .isEqualTo("intriguing");
    }

    @Test
    public void decoratedOnDemandWithHandleInTransaction() throws Exception {
        OnDemandOddities onDemand = new DecoratedOnDemandOddities();
        onDemand.withHandle(h ->
            h.inTransaction(txn ->
                assertThat(onDemand.versionA().value()).isEqualTo("intriguing")));
    }

    @Test
    public void decoratedOnDemandInTransaction() throws Exception {
        OnDemandOddities onDemand = new DecoratedOnDemandOddities();
        onDemand.inTransaction(txn ->
                assertThat(onDemand.versionA().value()).isEqualTo("intriguing"));
    }
}
