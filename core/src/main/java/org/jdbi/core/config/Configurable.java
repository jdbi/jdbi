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
package org.jdbi.core.config;

import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

import org.jdbi.core.argument.ArgumentFactory;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.argument.QualifiedArgumentFactory;
import org.jdbi.core.array.SqlArrayArgumentStrategy;
import org.jdbi.core.array.SqlArrayType;
import org.jdbi.core.array.SqlArrayTypeFactory;
import org.jdbi.core.array.SqlArrayTypes;
import org.jdbi.core.codec.CodecFactory;
import org.jdbi.core.collector.CollectorFactory;
import org.jdbi.core.collector.JdbiCollectors;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMapperFactory;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.MapEntryMappers;
import org.jdbi.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.statement.SqlLogger;
import org.jdbi.core.statement.SqlParser;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.StatementCustomizer;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.core.statement.TimingCollector;
import org.jdbi.meta.Beta;
import org.jdbi.core.statement.internal.DefineNamedBindingsStatementCustomizer;

/**
 * A type with access to access and modify arbitrary Jdbi configuration.
 *
 * @param <This> The subtype that implements this interface.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface Configurable<This> {

    /**
     * Returns the configuration registry associated with this object.
     *
     * @return the configuration registry associated with this object.
     */
    ConfigRegistry getConfig();

    /**
     * Gets the configuration object of the given type, associated with this object.
     *
     * @param configClass the configuration type
     * @param <C>         the configuration type
     * @return the configuration object of the given type, associated with this object.
     */
    default <C extends JdbiConfig<C>> C getConfig(final Class<C> configClass) {
        return getConfig().get(configClass);
    }

    /**
     * Passes the configuration object of the given type to the configurer, then returns this object.
     *
     * @param configClass the configuration type
     * @param configurer  consumer that will be passed the configuration object
     * @param <C>         the configuration type
     * @return this object (for call chaining)
     */
    @SuppressWarnings("unchecked")
    default <C extends JdbiConfig<C>> This configure(final Class<C> configClass, final Consumer<C> configurer) {
        configurer.accept(getConfig(configClass));
        return (This) this;
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).setTemplateEngine(rewriter)}
     *
     * @param templateEngine the template engine
     * @return this
     */
    default This setTemplateEngine(final TemplateEngine templateEngine) {
        return configure(SqlStatements.class, c -> c.setTemplateEngine(templateEngine));
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).setSqlParser(rewriter)}
     *
     * @param parser SQL parser
     * @return this
     */
    default This setSqlParser(final SqlParser parser) {
        return configure(SqlStatements.class, c -> c.setSqlParser(parser));
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).setTimingCollector(collector)}
     *
     * @param collector timing collector
     * @return this
     * @deprecated use {@link #setSqlLogger} instead
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    default This setTimingCollector(final TimingCollector collector) {
        return configure(SqlStatements.class, c -> c.setTimingCollector(collector));
    }

    default This setSqlLogger(final SqlLogger sqlLogger) {
        return configure(SqlStatements.class, c -> c.setSqlLogger(sqlLogger));
    }

    default This addCustomizer(final StatementCustomizer customizer) {
        return configure(SqlStatements.class, c -> c.addCustomizer(customizer));
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).define(key, value)}
     *
     * @param key   attribute name
     * @param value attribute value
     * @return this
     */
    default This define(final String key, final Object value) {
        return configure(SqlStatements.class, c -> c.define(key, value));
    }

    /**
     * Convenience method for {@code getConfig(Arguments.class).register(factory)}
     *
     * @param factory argument factory
     * @return this
     */
    default This registerArgument(final ArgumentFactory factory) {
        return configure(Arguments.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(Arguments.class).register(factory)}
     *
     * @param factory qualified argument factory
     * @return this
     */
    default This registerArgument(final QualifiedArgumentFactory factory) {
        return configure(Arguments.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).setArgumentStrategy(strategy)}
     *
     * @param strategy argument strategy
     * @return this
     */
    default This setSqlArrayArgumentStrategy(final SqlArrayArgumentStrategy strategy) {
        return configure(SqlArrayTypes.class, c -> c.setArgumentStrategy(strategy));
    }

    /**
     * Convenience method for {@code getConfig(MapEntryMappers.class).setKeyColumn(keyColumn)}
     *
     * @param keyColumn the key column name
     * @return this
     */
    default This setMapKeyColumn(final String keyColumn) {
        return configure(MapEntryMappers.class, c -> c.setKeyColumn(keyColumn));
    }

    /**
     * Convenience method for {@code getConfig(MapEntryMappers.class).setValueColumn(valueColumn)}
     *
     * @param valueColumn the value column name
     * @return this
     */
    default This setMapValueColumn(final String valueColumn) {
        return configure(MapEntryMappers.class, c -> c.setValueColumn(valueColumn));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(elementType, sqlTypeName)}
     *
     * @param elementType element type
     * @param sqlTypeName SQL type name
     * @return this
     */
    default This registerArrayType(final Class<?> elementType, final String sqlTypeName) {
        return configure(SqlArrayTypes.class, c -> c.register(elementType, sqlTypeName));
    }

    /**
     * Convenience method for registering an array type as {@link SqlArrayTypeFactory#of(Class, String, Function)}.
     *
     * @param elementType element raw type
     * @param sqlTypeName SQL type name
     * @param conversion  the function to convert to database representation
     * @param <T>         element type
     * @return this
     */
    default <T> This registerArrayType(final Class<T> elementType, final String sqlTypeName, final Function<T, ?> conversion) {
        return registerArrayType(SqlArrayTypeFactory.of(elementType, sqlTypeName, conversion));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(arrayType)}
     *
     * @param arrayType SQL array type
     * @return this
     */
    default This registerArrayType(final SqlArrayType<?> arrayType) {
        return configure(SqlArrayTypes.class, c -> c.register(arrayType));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(factory)}
     *
     * @param factory SQL array type factory
     * @return this
     */
    default This registerArrayType(final SqlArrayTypeFactory factory) {
        return configure(SqlArrayTypes.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(JdbiCollectors.class).register(CollectorFactory.collectorFactory(collectionType, collector))}
     *
     * @param collectionType collector type to register the collector for
     * @param collector      the Collector to use to build the resulting collection
     * @return this
     * @since 3.38.0
     */
    default This registerCollector(final Type collectionType, final Collector<?, ?, ?> collector) {
        return configure(JdbiCollectors.class, c -> c.registerCollector(collectionType, collector));
    }

    /**
     * Convenience method for {@code getConfig(JdbiCollectors.class).register(factory)}
     *
     * @param factory collector factory
     * @return this
     */
    default This registerCollector(final CollectorFactory factory) {
        return configure(JdbiCollectors.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(mapper)}
     *
     * @param mapper column mapper
     * @return this
     */
    default This registerColumnMapper(final ColumnMapper<?> mapper) {
        return configure(ColumnMappers.class, c -> c.register(mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(type, mapper)}
     *
     * @param <T>    the type
     * @param type   the generic type to register
     * @param mapper the mapper to use on that type
     * @return this
     */
    default <T> This registerColumnMapper(final GenericType<T> type, final ColumnMapper<T> mapper) {
        return configure(ColumnMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(type, mapper)}
     *
     * @param type   the type to register
     * @param mapper the mapper to use on that type
     * @return this
     */
    default This registerColumnMapper(final Type type, final ColumnMapper<?> mapper) {
        return configure(ColumnMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(type, mapper)}
     *
     * @param type   the type to register
     * @param mapper the mapper to use on that type
     * @return this
     */
    default <T> This registerColumnMapper(final QualifiedType<T> type, final ColumnMapper<T> mapper) {
        return configure(ColumnMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(factory)}
     *
     * @param factory column mapper factory
     * @return this
     */
    default This registerColumnMapper(final ColumnMapperFactory factory) {
        return configure(ColumnMappers.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(factory)}
     *
     * @param factory column mapper factory
     * @return this
     */
    default This registerColumnMapper(final QualifiedColumnMapperFactory factory) {
        return configure(ColumnMappers.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(Extensions.class).register(factory)}
     *
     * @param factory extension factory
     * @return this
     */
    default This registerExtension(final ExtensionFactory factory) {
        return configure(Extensions.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(mapper)}
     *
     * @param mapper row mapper
     * @return this
     */
    default This registerRowMapper(final RowMapper<?> mapper) {
        return configure(RowMappers.class, c -> c.register(mapper));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(type, mapper)}
     *
     * @param <T>    the type
     * @param type   to match
     * @param mapper row mapper
     * @return this
     */
    default <T> This registerRowMapper(final GenericType<T> type, final RowMapper<T> mapper) {
        return configure(RowMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(type, mapper)}
     *
     * @param type   to match
     * @param mapper row mapper
     * @return this
     */
    default This registerRowMapper(final Type type, final RowMapper<?> mapper) {
        return configure(RowMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(factory)}
     *
     * @param factory row mapper factory
     * @return this
     */
    default This registerRowMapper(final RowMapperFactory factory) {
        return configure(RowMappers.class, c -> c.register(factory));
    }

    /**
     * Convenience method to register a {@link CodecFactory}.
     *
     * @param codecFactory codec factory
     * @return this
     */
    @Beta
    default This registerCodecFactory(final CodecFactory codecFactory) {
        registerColumnMapper(codecFactory);
        return registerArgument(codecFactory);
    }

    /**
     * Define all bound arguments that don't already have a definition with a boolean indicating their presence.
     * Useful to easily template optional properties of pojos or beans like {@code <if(property)>property = :property<endif>}.
     * @return this
     */
    @Beta
    default This defineNamedBindings() {
        return addCustomizer(new DefineNamedBindingsStatementCustomizer());
    }
}

