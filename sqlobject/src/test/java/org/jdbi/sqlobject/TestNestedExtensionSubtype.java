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

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMapperFactory;
import org.jdbi.sqlobject.config.RegisterColumnMapperFactory;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class TestNestedExtensionSubtype {
    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    Jdbi jdbi;

    @BeforeEach
    void setUp() {
        jdbi = h2Extension.getJdbi();
        jdbi.registerExtension(new ExtensionFactory() {
            @Override
            public boolean accepts(final Class<?> extensionType) {
                return Supertype.class.equals(extensionType);
            }

            @Override
            public Set<FactoryFlag> getFactoryFlags() {
                return EnumSet.of(FactoryFlag.DONT_USE_PROXY);
            }

            @Override
            public <E> E attach(final Class<E> extensionType, final HandleSupplier handleSupplier) {
                return extensionType.cast(handleSupplier.getHandle().attach(Subtype.class));
            }
        });
    }

    @Test
    public void testOverrideMethodAttach() {
        jdbi.useHandle(h ->
                assertThat(h.attach(Supertype.class).overrideInt())
                        .isInstanceOf(SubInt.class)
                        .extracting(SuperInt::value)
                        .isEqualTo(42));
    }

    @Test
    public void testOverrideMethodOnDemand() {
        assertThat(jdbi.onDemand(Supertype.class).overrideInt())
                .isInstanceOf(SubInt.class)
                .extracting(SuperInt::value)
                .isEqualTo(42);
    }

    @RegisterColumnMapperFactory(SuperIntFactory.class)
    interface Supertype {
        @SqlQuery("select 41")
        SuperInt overrideInt();
    }

    interface Subtype extends Supertype {
        @Override
        @SqlQuery("select 42")
        SubInt overrideInt();
    }

    static class SuperInt {
        final int value;

        SuperInt(final int value) {
            this.value = value;
        }

        int value() {
            return value;
        }
    }

    // Use a varying return type to make sure e.g. bridge methods also work
    static class SubInt extends SuperInt {
        SubInt(final int value) {
            super(value);
        }
    }

    public static class SuperIntFactory implements ColumnMapperFactory {
        @Override
        public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
            if (SuperInt.class.equals(type)) {
                return Optional.of((r, c, ctx) -> new SuperInt(r.getInt(c)));
            } else if (SubInt.class.equals(type)) {
                return Optional.of((r, c, ctx) -> new SubInt(r.getInt(c)));
            }
            return Optional.empty();
        }
    }
}
