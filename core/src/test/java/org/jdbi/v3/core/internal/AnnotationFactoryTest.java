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
package org.jdbi.v3.core.internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.qualifier.Qualifier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnnotationFactoryTest {
    @Test
    @Foo
    public void createNoProperties() throws Exception {
        checkImplementation(Foo.class, "createNoProperties", AnnotationFactory.create(Foo.class),
                "@org.jdbi.v3.core.internal.AnnotationFactoryTest$Foo()");
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {}

    @Test
    public void createAnnotationWithAttributeFails() {
        assertThatThrownBy(() -> AnnotationFactory.create(Bar.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot synthesize annotation @Bar from Bar.class because it has attribute");
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Bar {
        int value();
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Baz {
        String value();
        int coolness() default 42;
        Class<?> soMeta() default Baz.class;
    }

    @Test
    @Baz("baz")
    public void createWithDefaults() throws Exception {
        checkImplementation(Baz.class, "createWithDefaults", AnnotationFactory.create(Baz.class, Collections.singletonMap("value", "baz")),
                "@org.jdbi.v3.core.internal.AnnotationFactoryTest$Baz(value=baz)");
    }

    @Test
    @Baz(value = "baz", coolness = -1, soMeta = Foo.class)
    public void createWithExplicitProperties() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("value", "baz");
        values.put("coolness", -1);
        values.put("soMeta", Foo.class);
        checkImplementation(Baz.class, "createWithExplicitProperties", AnnotationFactory.create(Baz.class, values),
                "@org.jdbi.v3.core.internal.AnnotationFactoryTest$Baz(coolness=-1, soMeta=interface org.jdbi.v3.core.internal.AnnotationFactoryTest$Foo, value=baz)");
    }

    @Test
    public void unequal() throws Exception {
        assertThat(AnnotationFactory.create(Baz.class, Collections.singletonMap("value", "baz")))
            .isNotEqualTo(getAnno(Baz.class, "createWithExplicitProperties"));
    }

    private <A extends Annotation> void checkImplementation(Class<A> annoType, String annotatedMethod, A synthetic, String expectedToString)
    throws NoSuchMethodException {
        A real = getAnno(annoType, annotatedMethod);

        assertThat(real).describedAs("real equals synthetic").isEqualTo(synthetic);
        assertThat(synthetic).describedAs("synthetic equals real").isEqualTo(real);
        assertThat(synthetic.hashCode()).describedAs("hashCode").isEqualTo(real.hashCode());
        assertThat(synthetic).describedAs("toString").hasToString(expectedToString);
        assertThat(synthetic.annotationType()).describedAs("annotationType").isEqualTo(annoType);
    }

    private <A extends Annotation> A getAnno(Class<A> annoType, String annotatedMethod) throws NoSuchMethodException {
        return getClass()
            .getMethod(annotatedMethod)
            .getAnnotation(annoType);
    }
}
