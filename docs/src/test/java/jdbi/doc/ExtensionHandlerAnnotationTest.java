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
import java.util.stream.Stream;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionHandlerAnnotationTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());

    Jdbi jdbi;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

        // tag::register-factory[]
        jdbi.configure(Extensions.class, extensions ->  // <1>
                extensions.register(extensionType ->
                        Stream.of(extensionType.getMethods())
                                .anyMatch(method ->
                                        Stream.of(method.getAnnotations())
                                                .map(annotation -> annotation.annotationType()
                                                        .getAnnotation(UseExtensionHandler.class)) // <2>
                                                .anyMatch(a -> a != null && "test".equals(a.id())) // <3>
                                )));
        // end::register-factory[]
    }

    @Test
    public void testSomething() {
        Something result = jdbi.withExtension(ExtensionType.class, e -> e.getSomething(2, "banana"));

        assertThat(result).isEqualTo(new Something(2, "banana"));
    }

    // tag::annotation-code[]
    interface ExtensionType {

        @SomethingAnnotation // <4>
        Something getSomething(int id, String name);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD}) // <5>
    @UseExtensionHandler(id = "test", // <6>
            value = SomethingExtensionHandler.class) // <7>
    @interface SomethingAnnotation {}

    public static class SomethingExtensionHandler implements ExtensionHandler.Simple {
        @Override
        public Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception {
            return new Something((int) args[0], (String) args[1]);
        }
    }
    // end::annotation-code[]
}
