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
package org.jdbi.v3.guice;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.guava.codec.TypeResolvingCodecFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JDBI Factory creates a new {@link Jdbi} instance.
 */
public final class GuiceFactory {

    private final Set<RowMapper<?>> rowMappers;
    private final Set<ColumnMapper<?>> columnMappers;
    private final Set<Function<Jdbi, Jdbi>> transformers;
    private final Map<Class<?>, String> arrayTypes;
    private final Set<JdbiPlugin> plugins;
    private final CodecFactory codecFactory;

    @Inject
    GuiceFactory(final Set<RowMapper<?>> rowMappers,
        final Set<ColumnMapper<?>> columnMappers,
        final Set<Function<Jdbi, Jdbi>> transformers,
        final Map<Class<?>, String> arrayTypes,
        final Set<JdbiPlugin> plugins,
        final Map<QualifiedType<?>, Codec<?>> codecs) {
        this.rowMappers = ImmutableSet.copyOf(checkNotNull(rowMappers, "rowMappers is null"));
        this.columnMappers = ImmutableSet.copyOf(checkNotNull(columnMappers, "columnMappers is null"));
        this.transformers = ImmutableSet.copyOf(checkNotNull(transformers, "transformers is null"));
        this.arrayTypes = checkNotNull(arrayTypes, "arrayTypes is null");
        this.plugins = ImmutableSet.copyOf(checkNotNull(plugins, "plugins is null"));

        this.codecFactory = new TypeResolvingCodecFactory(checkNotNull(codecs, "codecs is null"));
    }

    public Jdbi create(DataSource dataSource) {
        return create(() -> Jdbi.create(dataSource));
    }

    public Jdbi create(Supplier<Jdbi> jdbiFactory) {

        Jdbi jdbi = jdbiFactory.get();

        rowMappers.forEach(jdbi::registerRowMapper);
        columnMappers.forEach(jdbi::registerColumnMapper);

        jdbi.registerColumnMapper(codecFactory);
        jdbi.registerArgument(codecFactory);

        plugins.forEach(jdbi::installPlugin);
        arrayTypes.forEach(jdbi::registerArrayType);

        for (Function<Jdbi, Jdbi> transformer : transformers) {
            jdbi = transformer.apply(jdbi);
        }

        return jdbi;
    }
}
