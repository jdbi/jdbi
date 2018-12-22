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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.jdbi.v3.core.qualifier.Qualifier;
import org.junit.Test;

public class AnnotationFactoryTest {
    @Test
    @Foo
    public void create() throws Exception {
        Foo synthetic = AnnotationFactory.create(Foo.class);
        Foo real = getClass()
            .getMethod("create")
            .getAnnotation(Foo.class);

        assertThat(real).isEqualTo(synthetic);
        assertThat(synthetic).isEqualTo(real);
        assertThat(synthetic.hashCode()).isEqualTo(real.hashCode());
        assertThat(synthetic.toString()).isEqualTo(real.toString());
        assertThat(synthetic.annotationType()).isEqualTo(Foo.class);
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {}

    @Test
    public void createAnnotationWithAttributeFails() {
        assertThatThrownBy(() -> AnnotationFactory.create(Bar.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot synthesize annotation @Bar from Bar.class because it has attributes");
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Bar {
        int value();
    }
}
