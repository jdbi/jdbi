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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MapperCacheTest {
    @RegisterExtension
    public H2DatabaseExtension h2db = H2DatabaseExtension.instance();

    @Test
    void notRegisteredFail() {
        assertThatThrownBy(() ->
                h2db.getSharedHandle()
                    .createQuery("select 1")
                    .mapTo(CustomType.class))
            .isInstanceOf(NoSuchMapperException.class);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void jdbiColumnMapperCached() {
        final AtomicInteger builds = new AtomicInteger(0);
        final Jdbi jdbi = h2db.getJdbi();
        jdbi.registerColumnMapper(new ColumnMapperFactory() {
            @Override
            public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
                if (type == CustomType.class) {
                    builds.incrementAndGet();
                    return Optional.of((r, n, c) -> new CustomType());
                }
                return Optional.empty();
            }
        });
        for (int i = 0; i < 4; i++) {
            jdbi.useHandle(h -> {
                assertThat(h.createQuery("select 1")
                        .mapTo(CustomType.class))
                    .isNotNull();
            });
        }
        assertThat(builds.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void jdbiRowMapperCached() {
        final AtomicInteger builds = new AtomicInteger(0);
        final Jdbi jdbi = h2db.getJdbi();
        jdbi.registerRowMapper(new RowMapperFactory() {
            @Override
            public Optional<RowMapper<?>> build(final Type type, final ConfigRegistry config) {
                if (type == CustomType.class) {
                    builds.incrementAndGet();
                    return Optional.of((r, c) -> new CustomType());
                }
                return Optional.empty();
            }
        });
        for (int i = 0; i < 4; i++) {
            jdbi.useHandle(h -> {
                assertThat(h.createQuery("select 1")
                        .mapTo(CustomType.class))
                    .isNotNull();
            });
        }
        assertThat(builds.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void handleColumnMapperCached() {
        final AtomicInteger builds = new AtomicInteger(0);
        h2db.getJdbi().useHandle(h -> {
            h.registerColumnMapper(new ColumnMapperFactory() {
                @Override
                public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
                    if (type == CustomType.class) {
                        builds.incrementAndGet();
                        return Optional.of((r, n, c) -> new CustomType());
                    }
                    return Optional.empty();
                }
            });

            for (int i = 0; i < 4; i++) {
                assertThat(h.createQuery("select 1")
                        .mapTo(CustomType.class))
                    .isNotNull();
            }
        });

        assertThat(builds.get()).isEqualTo(1);
    }

    @Test
    @Disabled("https://github.com/jdbi/jdbi/issues/2406")
    void handleRowMapperCached() {
        final AtomicInteger builds = new AtomicInteger(0);
        h2db.getJdbi().useHandle(h -> {
            h.registerRowMapper(new RowMapperFactory() {
                @Override
                public Optional<RowMapper<?>> build(final Type type, final ConfigRegistry config) {
                    if (type == CustomType.class) {
                        builds.incrementAndGet();
                        return Optional.of((r, c) -> new CustomType());
                    }
                    return Optional.empty();
                }
            });
            for (int i = 0; i < 4; i++) {
                assertThat(h.createQuery("select 1")
                        .mapTo(CustomType.class))
                    .isNotNull();
            }
        });

        assertThat(builds.get()).isEqualTo(1);
    }

    class CustomType {}
}
