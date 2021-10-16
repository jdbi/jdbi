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

package org.jdbi.v3.core.argument;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PrimitivesArgumentFactoryTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void getHandle() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void bindingNullToPrimitiveThrows() {
        assertThat(handle.getConfig(Arguments.class).isBindingNullToPrimitivesPermitted()).isTrue();

        assertThat(handle.createQuery("select :foo").bindByType("foo", null, int.class).mapTo(int.class).one())
            .describedAs("binding a null binds the primitive's default")
            .isZero();

        handle.getConfig(Arguments.class).setBindingNullToPrimitivesPermitted(false);

        assertThatThrownBy(() -> handle.createQuery("select :foo").bindByType("foo", null, int.class).mapTo(int.class).one())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("binding null to a primitive int is forbidden by configuration, declare a boxed type instead");

        assertThat(handle.createQuery("select :foo").bindByType("foo", null, Integer.class).mapTo(Integer.class).one())
            .describedAs("binding a null to a boxed type is fine")
            .isNull();

        assertThat(handle.createQuery("select :foo").bindByType("foo", null, Long.class).mapTo(Long.class).one())
            .describedAs("binding a null to a boxed type is fine")
            .isNull();
    }
}
