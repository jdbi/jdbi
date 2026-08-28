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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.meta.Legacy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.bar;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.foo;

public class TestQualifiedType {
    @Test
    public void testQualifiedType() {
        assertThat(QualifiedType.of(String.class).with(NVarchar.class))
            .isEqualTo(QualifiedType.of(String.class).with(NVarchar.class))
            .hasSameHashCodeAs(QualifiedType.of(String.class).with(NVarchar.class))
            .hasToString("@org.jdbi.v3.core.qualifier.NVarchar() java.lang.String");

        assertThat(QualifiedType.of(int.class))
            .isEqualTo(QualifiedType.of(int.class))
            .hasSameHashCodeAs(QualifiedType.of(int.class))
            .hasToString("int");

        assertThat(QualifiedType.of(new GenericType<List<String>>() {}))
            .isEqualTo(QualifiedType.of(new GenericType<List<String>>() {}))
            .hasSameHashCodeAs(QualifiedType.of(new GenericType<List<String>>() {}))
            .hasToString("java.util.List<java.lang.String>");

        assertThat(QualifiedType.of(String.class).with(foo(1), bar("1")))
            .isEqualTo(QualifiedType.of(String.class).with(foo(1), bar("1")))
            .isEqualTo(QualifiedType.of(String.class).with(bar("1"), foo(1)))
            .hasSameHashCodeAs(QualifiedType.of(String.class).with(foo(1), bar("1")))
            .hasSameHashCodeAs(QualifiedType.of(String.class).with(bar("1"), foo(1)))
            .isNotEqualTo(QualifiedType.of(int.class).with(bar("1"), foo(1)))
            .isNotEqualTo(QualifiedType.of(String.class).with(bar("2"), foo(1)))
            .isNotEqualTo(QualifiedType.of(String.class).with(bar("1"), foo(2)))
            .isNotEqualTo(QualifiedType.of(String.class).with(foo(1)))
            .isNotEqualTo(QualifiedType.of(String.class).with(bar("1")));
    }

    @Test
    public void memberlessQualifierClassEqualsRealAnnotation() {
        Annotation real = Holder.class.getAnnotation(Legacy.class);
        QualifiedType<String> fromClass = QualifiedType.of(String.class).with(Legacy.class);
        QualifiedType<String> fromInstance = QualifiedType.of(String.class).with(real);

        assertThat(fromClass)
            .isEqualTo(fromInstance)
            .hasSameHashCodeAs(fromInstance)
            .hasToString("@org.jdbi.v3.meta.Legacy() java.lang.String");
        assertThat(fromClass.hasQualifier(Legacy.class)).isTrue();
        assertThat(fromClass.hasQualifier(NVarchar.class)).isFalse();
        assertThat(fromClass.hasQualifiers(Set.of(real))).isTrue();
        assertThat(fromInstance.hasQualifiers(Set.of(real))).isTrue();
        assertThat(fromClass.hasQualifiers(Set.of())).isFalse();
        assertThat(QualifiedType.of(String.class).hasQualifiers(Set.of())).isTrue();
        assertThat(QualifiedType.of(String.class).hasQualifiers(Set.of(real))).isFalse();
    }

    @Test
    public void getQualifiersSynthesizesMemberlessAnnotations() {
        Set<Annotation> qualifiers = QualifiedType.of(String.class).with(Legacy.class).getQualifiers();

        assertThat(qualifiers).hasSize(1);
        Annotation synthesized = qualifiers.iterator().next();
        assertThat(synthesized).isInstanceOf(Legacy.class);
        assertThat(synthesized).isEqualTo(Holder.class.getAnnotation(Legacy.class));
        assertThat(qualifiers).isEqualTo(Set.of(Holder.class.getAnnotation(Legacy.class)));
    }

    @Test
    public void qualifierWithMembersComparesByValue() {
        Annotation real = DefaultedHolder.class.getAnnotation(Defaulted.class);
        Annotation synthesized = AnnotationFactory.create(Defaulted.class, Map.of("value", 7));
        Annotation other = AnnotationFactory.create(Defaulted.class, Map.of("value", 8));

        assertThat(QualifiedType.of(String.class).with(real))
            .isEqualTo(QualifiedType.of(String.class).with(synthesized))
            .hasSameHashCodeAs(QualifiedType.of(String.class).with(synthesized))
            .isNotEqualTo(QualifiedType.of(String.class).with(other));
        assertThat(QualifiedType.of(String.class).with(real).getQualifiers()).containsExactly(real);
        assertThat(QualifiedType.of(String.class).with(real).hasQualifiers(Set.of(synthesized))).isTrue();
        assertThat(QualifiedType.of(String.class).with(real).hasQualifiers(Set.of(other))).isFalse();
    }

    @Test
    public void qualifierClassWithRequiredMemberIsRejected() {
        assertThatThrownBy(() -> QualifiedType.of(String.class).with(SampleQualifiers.Foo.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot synthesize annotation @Foo");
    }

    @Test
    public void qualifierClassWithDefaultedMembersIsSynthesized() {
        assertThat(QualifiedType.of(String.class).with(Defaulted.class))
            .isEqualTo(QualifiedType.of(String.class).with(DefaultedHolder.class.getAnnotation(Defaulted.class)))
            .isEqualTo(QualifiedType.of(String.class).with(Defaulted.class));
    }

    @Legacy
    private static final class Holder {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Qualifier
    public @interface Defaulted {
        int value() default 7;
    }

    @Defaulted
    private static final class DefaultedHolder {}
}
