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
package org.jdbi.v3.guice.internal;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.guava.codec.TypeResolvingCodecFactory;
import org.jdbi.v3.guice.GuiceJdbiCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Customizer contributing all customizations from a module to a Jdbi instance.
 */
@Singleton
public class InternalGuiceJdbiCustomizer implements GuiceJdbiCustomizer {

    static final TypeLiteral<Set<RowMapper<?>>> ROW_MAPPER_TYPE_LITERAL = new TypeLiteral<Set<RowMapper<?>>>() {};
    static final TypeLiteral<Map<Type, RowMapper<?>>> QUALIFIED_ROW_MAPPER_TYPE_LITERAL = new TypeLiteral<Map<Type, RowMapper<?>>>() {};
    static final TypeLiteral<Set<ColumnMapper<?>>> COLUMN_MAPPER_TYPE_LITERAL = new TypeLiteral<Set<ColumnMapper<?>>>() {};
    static final TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>> QUALIFIED_COLUMN_MAPPER_TYPE_LITERAL = new TypeLiteral<Map<QualifiedType<?>, ColumnMapper<?>>>() {};
    static final TypeLiteral<Set<GuiceJdbiCustomizer>> CUSTOMIZER_TYPE_LITERAL = new TypeLiteral<Set<GuiceJdbiCustomizer>>() {};
    static final TypeLiteral<Map<Class<?>, String>> ARRAY_TYPES_TYPE_LITERAL = new TypeLiteral<Map<Class<?>, String>>() {};
    static final TypeLiteral<Set<JdbiPlugin>> PLUGINS_TYPE_LITERAL = new TypeLiteral<Set<JdbiPlugin>>() {};
    static final TypeLiteral<Map<QualifiedType<?>, Codec<?>>> CODECS_TYPE_LITERAL = new TypeLiteral<Map<QualifiedType<?>, Codec<?>>>() {};

    private final Set<RowMapper<?>> rowMappers;
    private final Map<Type, RowMapper<?>> qualifiedRowMappers;
    private final Set<ColumnMapper<?>> columnMappers;
    private final Map<QualifiedType<?>, ColumnMapper<?>> qualifiedColumnMappers;
    private final Set<GuiceJdbiCustomizer> customizers;
    private final Map<Class<?>, String> arrayTypes;
    private final Set<JdbiPlugin> plugins;
    private final CodecFactory codecFactory;

    @Inject
    InternalGuiceJdbiCustomizer(final Set<RowMapper<?>> rowMappers,
        final Map<Type, RowMapper<?>> qualifiedRowMappers,
        final Set<ColumnMapper<?>> columnMappers,
        final Map<QualifiedType<?>, ColumnMapper<?>> qualifiedColumnMappers,
        final Set<GuiceJdbiCustomizer> customizers,
        final Map<Class<?>, String> arrayTypes,
        final Set<JdbiPlugin> plugins,
        final Map<QualifiedType<?>, Codec<?>> codecs) {
        this.rowMappers = ImmutableSet.copyOf(checkNotNull(rowMappers, "rowMappers is null"));
        this.qualifiedRowMappers = checkNotNull(qualifiedRowMappers, "qualifiedRowMappers is null");
        this.columnMappers = ImmutableSet.copyOf(checkNotNull(columnMappers, "columnMappers is null"));
        this.qualifiedColumnMappers = checkNotNull(qualifiedColumnMappers, "qualifiedColumnMappers is null");
        this.customizers = ImmutableSet.copyOf(checkNotNull(customizers, "customizers is null"));
        this.arrayTypes = checkNotNull(arrayTypes, "arrayTypes is null");
        this.plugins = ImmutableSet.copyOf(checkNotNull(plugins, "plugins is null"));
        this.codecFactory = new TypeResolvingCodecFactory(checkNotNull(codecs, "codecs is null"));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void customize(Jdbi jdbi) {
        rowMappers.forEach(jdbi::registerRowMapper);
        qualifiedRowMappers.forEach(jdbi::registerRowMapper);

        columnMappers.forEach(jdbi::registerColumnMapper);
        qualifiedColumnMappers.forEach((k, v) -> jdbi.registerColumnMapper((QualifiedType) k, (ColumnMapper) v));

        jdbi.registerColumnMapper(codecFactory);
        jdbi.registerArgument(codecFactory);

        plugins.forEach(jdbi::installPlugin);

        arrayTypes.forEach(jdbi::registerArrayType);
        customizers.forEach(c -> c.customize(jdbi));
    }
}
