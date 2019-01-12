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

import org.jdbi.v3.core.qualifier.SampleQualifiers.Bar;
import org.jdbi.v3.core.qualifier.SampleQualifiers.Foo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.bar;
import static org.jdbi.v3.core.qualifier.SampleQualifiers.foo;

public class TestQualifiers {
    @Test
    public void getQualifiers() throws Exception {
        assertThat(foo(1))
            .isEqualTo(foo(1))
            .isNotEqualTo(foo(2));

        assertThat(Qualifiers.getQualifiers(getClass().getDeclaredField("qualifiedField")))
            .containsExactly(foo(1));

        assertThat(Qualifiers.getQualifiers(getClass().getDeclaredMethod("qualifiedMethod")))
            .containsExactly(foo(2));

        assertThat(Qualifiers.getQualifiers(getClass().getDeclaredMethod("qualifiedParameter", String.class).getParameters()[0]))
            .containsExactly(foo(3));

        assertThat(Qualifiers.getQualifiers(QualifiedClass.class))
            .containsExactly(foo(4));

        assertThat(Qualifiers.getQualifiers(QualifiedClass.class.getDeclaredConstructor(String.class).getParameters()[0]))
            .containsExactly(foo(5));

        assertThat(Qualifiers.getQualifiers(getClass().getDeclaredField("twoQualifiers")))
            .containsExactlyInAnyOrder(foo(6), bar("six"));
    }

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
