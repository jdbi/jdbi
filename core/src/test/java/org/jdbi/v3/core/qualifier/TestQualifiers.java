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
package org.jdbi.v3.core.qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.qualifier.SampleQualifiers.Bar;
import org.jdbi.v3.core.qualifier.SampleQualifiers.Foo;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singleton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.bar;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.foo;

public class TestQualifiers {
    private Qualifiers qualifiers;

    @Before
    public void before() {
        qualifiers = new Qualifiers();
    }

    @Test
    public void getQualifiers() throws Exception {
        assertThat(foo(1))
            .isEqualTo(foo(1))
            .isNotEqualTo(foo(2));

        assertThat(qualifiers.findFor(WithQualifiers.class.getDeclaredField("qualifiedField")))
            .containsExactly(foo(1));

        assertThat(qualifiers.findFor(WithQualifiers.class.getDeclaredMethod("qualifiedMethod")))
            .containsExactly(foo(2));

        assertThat(qualifiers.findFor(WithQualifiers.class.getDeclaredMethod("qualifiedParameter", String.class).getParameters()[0]))
            .containsExactly(foo(3));

        assertThat(qualifiers.findFor(WithQualifiers.QualifiedClass.class))
            .containsExactly(foo(4));

        assertThat(qualifiers.findFor(WithQualifiers.QualifiedClass.class.getDeclaredConstructor(String.class).getParameters()[0]))
            .containsExactly(foo(5));

        assertThat(qualifiers.findFor(WithQualifiers.class.getDeclaredField("twoQualifiers")))
            .containsExactlyInAnyOrder(foo(6), bar("six"));
    }

    @Test
    public void singleQualified() throws NoSuchMethodException {
        Set<Annotation> annos = qualifiers.findFor(WithQualified.class.getMethod("nonNull"));

        assertThat(annos)
            .extracting(Annotation::annotationType)
            .containsExactly((Class) Nonnull.class);
    }

    @Test
    public void multipleQualified() throws NoSuchMethodException {
        Set<Annotation> annos = qualifiers.findFor(WithQualified.class.getMethod("both"));

        assertThat(annos)
            .extracting(Annotation::annotationType)
            .containsExactlyInAnyOrder((Class) Nonnull.class, (Class) Nullable.class);
    }

    @Test
    public void addResolver() throws NoSuchMethodException {
        Method method = WithQualified.class.getMethod("none");

        Set<Annotation> annos = qualifiers.findFor(method);
        assertThat(annos).isEmpty();

        qualifiers.addResolver(element -> singleton(AnnotationFactory.create(Nonnull.class)));

        annos = qualifiers.findFor(method);
        assertThat(annos).extracting(Annotation::annotationType).containsExactly((Class) Nonnull.class);
    }

    public static class WithQualifiers {
        @Foo(1)
        private String qualifiedField;

        @Foo(2)
        private void qualifiedMethod() {}

        private void qualifiedParameter(@Foo(3) String param) {}

        @Foo(4)
        private static class QualifiedClass {
            QualifiedClass(@Foo(5) String param) {}
        }

        @Foo(6)
        @Bar("six")
        private String twoQualifiers;
    }

    public interface WithQualified {
        void none();

        @Qualified(Nonnull.class)
        void nonNull();

        @Qualified({Nonnull.class, Nullable.class})
        void both();
    }
}
