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
package org.jdbi.v3.core.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseCustomExtensionHandlerFactory {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        Jdbi db = h2Extension.getJdbi();
        db.configure(Extensions.class, e -> e.register(new ExtensionFrameworkTestFactory()));

        ExtensionHandlerFactory customExtensionHandlerFactory = new ExtensionHandlerFactory() {

            @Override
            public boolean accepts(Class<?> extensionType, Method method) {
                return true;
            }

            @Override
            public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
                return getImplementation(extensionType, method).map(m ->
                        (handleSupplier, target, args) -> m.invoke(null, Stream.concat(Stream.of(target), Stream.of(args)).toArray())
                );
            }

            private Optional<Method> getImplementation(Class<?> type, Method method) {
                return Stream.of(type.getClasses())
                        .filter(SomethingDao.DefaultImpls.class::isAssignableFrom)
                        .flatMap(c -> Stream.of(c.getMethods()).filter(m -> m.getName().equals(method.getName())))
                        .findAny();
            }
        };

        db.configure(Extensions.class, c -> c.registerHandlerFactory(customExtensionHandlerFactory));

        handle = db.open();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void shouldUseConfiguredDefaultHandler() {
        SomethingDao h = handle.attach(SomethingDao.class);
        Something s = h.insertAndFind(new Something(1, "Joy"));
        assertThat(s.getName()).isEqualTo("Joy");
    }

    public interface SomethingDao {

        @Insert
        void insert(Something s);

        @Retrieve
        Something findById(int id);

        Something insertAndFind(Something s);

        @SuppressWarnings("unused")
        class DefaultImpls {

            private DefaultImpls() {}

            public static Something insertAndFind(SomethingDao dao, Something s) {
                dao.insert(s);
                return dao.findById(s.getId());
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @UseExtensionHandler(id = "test", value = Insert.Impl.class)
    public @interface Insert {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception {
                Handle handle = handleSupplier.getHandle();
                try (Update update = handle.createUpdate("insert into something (id, name) values (:id, :name)")) {
                    return update.bindBean(args[0]).execute();
                }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @UseExtensionHandler(id = "test", value = Retrieve.Impl.class)
    public @interface Retrieve {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception {
                Handle handle = handleSupplier.getHandle();
                try (Query query = handle.createQuery("select id, name from something where id = :id")) {
                    return query.bind("id", args[0])
                            .map(new SomethingMapper())
                            .one();
                }
            }
        }
    }
}
