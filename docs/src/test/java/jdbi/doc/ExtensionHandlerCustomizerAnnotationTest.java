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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.extension.AttachedExtensionHandler;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.extension.annotation.UseExtensionHandler;
import org.jdbi.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionHandlerCustomizerAnnotationTest {

    public static final Logger LOG = LoggerFactory.getLogger(ExtensionHandlerCustomizerTest.class);

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());

    Jdbi jdbi;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

        jdbi.configure(Extensions.class, extensions ->
                extensions.register(extensionType ->
                        Stream.of(extensionType.getMethods())
                                .anyMatch(method -> Stream.of(method.getAnnotations())
                                        .map(annotation -> annotation.annotationType().getAnnotation(UseExtensionHandler.class))
                                        .anyMatch(a -> a != null && "test".equals(a.id())))));

        jdbi.registerRowMapper(new SomethingMapper());

        jdbi.useExtension(SomethingDao.class, dao -> {
            dao.insert(new Something(1, "apple"));
            dao.insert(new Something(2, "banana"));
            dao.insert(new Something(3, "coconut"));
            dao.insert(new Something(4, "date"));
        });
    }

    @Test
    public void testMethodSomething() {
        Something result = jdbi.withExtension(MethodExtensionType.class, e -> e.getSomething(2, "banana"));

        assertThat(result).isEqualTo(new Something(2, "banana"));
    }

    @Test
    public void testInstanceSomething() {
        Something result = jdbi.withExtension(InstanceExtensionType.class, e -> e.getSomething(1, "apple"));

        assertThat(result).isEqualTo(new Something(1, "apple"));
    }

    @Test
    public void testFindById() {
        Optional<Something> result = jdbi.withExtension(SomethingDao.class, dao -> dao.findSomething(1));

        assertThat(result)
                .isPresent()
                .contains(new Something(1, "apple"));

    }

    // tag::extension-types[]
    interface MethodExtensionType {

        @LogThis  // <2>
        @SomethingAnnotation
        Something getSomething(int id, String name);
    }

    @LogThis // <3>
    interface InstanceExtensionType {

        @SomethingAnnotation
        Something getSomething(int id, String name);
    }
    // end::extension-types[]

    // tag::mixed-annotation[]
    interface SomethingDao {

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        @LogThis
        Optional<Something> findSomething(int id);
    }
    // end::mixed-annotation[]

    // tag::annotation[]
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE}) // <1>
    @UseExtensionHandlerCustomizer(
            value = LoggingExtensionHandlerCustomizer.class)
    @interface LogThis {}
    // end::annotation[]

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD}) // <5>
    @UseExtensionHandler(id = "test", // <6>
            value = SomethingExtensionHandler.class) // <7>
    @interface SomethingAnnotation {}

    // tag::extension-handler-customizer[]
    public static class LoggingExtensionHandlerCustomizer implements ExtensionHandlerCustomizer {

        @Override
        public ExtensionHandler customize(ExtensionHandler handler, Class<?> extensionType, Method method) {
            return (config, target) -> {
                AttachedExtensionHandler delegate = handler.attachTo(config, target);
                return (handleSupplier, args) -> {
                    LOG.info(format("Entering %s on %s", method, extensionType.getSimpleName()));
                    try {
                        return delegate.invoke(handleSupplier, args);
                    } finally {
                        LOG.info(format("Leaving %s on %s", method, extensionType.getSimpleName()));
                    }
                };
            };
        }
    }
    // end::extension-handler-customizer[]

    public static class SomethingExtensionHandler implements ExtensionHandler.Simple {
        @Override
        public Object invoke(HandleSupplier handleSupplier, Object... args) {
            return new Something((int) args[0], (String) args[1]);
        }
    }

    static class SomethingMapper implements RowMapper<Something> {

        @Override
        public Something map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Something(
                    rs.getInt("id"),
                    rs.getString("name")
            );
        }
    }
}
