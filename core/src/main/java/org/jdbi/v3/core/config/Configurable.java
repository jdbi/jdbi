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
package org.jdbi.v3.core.config;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import org.jdbi.v3.core.statement.TimingCollector;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.array.SqlArrayArgumentStrategy;
import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypeFactory;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.core.statement.SqlStatements;

/**
 * A type with access to access and modify arbitrary JDBI configuration.
 *
 * @param <This> The subtype that implements this interface.
 */
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
    default <C extends JdbiConfig<C>> C getConfig(Class<C> configClass) {
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
    default <C extends JdbiConfig<C>> This configure(Class<C> configClass, Consumer<C> configurer) {
        configurer.accept(getConfig(configClass));
        return (This) this;
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).setStatementRewriter(rewriter)}
     *
     * @param rewriter statement rewriter
     * @return this
     */
    default This setStatementRewriter(StatementRewriter rewriter) {
        return configure(SqlStatements.class, c -> c.setStatementRewriter(rewriter));
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).setStatementRewriter(collector)}
     *
     * @param collector timing collector
     * @return this
     */
    default This setTimingCollector(TimingCollector collector) {
        return configure(SqlStatements.class, c -> c.setTimingCollector(collector));
    }

    /**
     * Convenience method for {@code getConfig(SqlStatements.class).define(key, value)}
     *
     * @param key   attribute name
     * @param value attribute value
     * @return this
     */
    default This define(String key, Object value) {
        return configure(SqlStatements.class, c -> c.define(key, value));
    }

    /**
     * Convenience method for {@code getConfig(Arguments.class).register(factory)}
     *
     * @param factory argument factory
     * @return this
     */
    default This registerArgument(ArgumentFactory factory) {
        return configure(Arguments.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).setArgumentStrategy(strategy)}
     *
     * @param strategy argument strategy
     * @return this
     */
    default This setSqlArrayArgumentStrategy(SqlArrayArgumentStrategy strategy) {
        return configure(SqlArrayTypes.class, c -> c.setArgumentStrategy(strategy));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(elementType, sqlTypeName)}
     *
     * @param elementType element type
     * @param sqlTypeName SQL type name
     * @return this
     */
    default This registerArrayType(Class<?> elementType, String sqlTypeName) {
        return configure(SqlArrayTypes.class, c -> c.register(elementType, sqlTypeName));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(arrayType)}
     *
     * @param arrayType SQL array type
     * @return this
     */
    default This registerArrayType(SqlArrayType<?> arrayType) {
        return configure(SqlArrayTypes.class, c -> c.register(arrayType));
    }

    /**
     * Convenience method for {@code getConfig(SqlArrayTypes.class).register(factory)}
     *
     * @param factory SQL array type factory
     * @return this
     */
    default This registerArrayType(SqlArrayTypeFactory factory) {
        return configure(SqlArrayTypes.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(JdbiCollectors.class).register(factory)}
     *
     * @param factory collector factory
     * @return this
     */
    default This registerCollector(CollectorFactory factory) {
        return configure(JdbiCollectors.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(mapper)}
     *
     * @param mapper column mapper
     * @return this
     */
    default This registerColumnMapper(ColumnMapper<?> mapper) {
        return configure(ColumnMappers.class, c -> c.register(mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(type, mapper)}
     *
     * @param type
     * @param mapper
     * @return
     */
    default This registerColumnMapper(Type type, ColumnMapper<?> mapper) {
        return configure(ColumnMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(ColumnMappers.class).register(factory)}
     *
     * @param factory column mapper factory
     * @return this
     */
    default This registerColumnMapper(ColumnMapperFactory factory) {
        return configure(ColumnMappers.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(Extensions.class).register(factory)}
     *
     * @param factory extension factory
     * @return this
     */
    default This registerExtension(ExtensionFactory factory) {
        return configure(Extensions.class, c -> c.register(factory));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(mapper)}
     *
     * @param mapper row mapper
     * @return this
     */
    default This registerRowMapper(RowMapper<?> mapper) {
        return configure(RowMappers.class, c -> c.register(mapper));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(type, mapper)}
     *
     * @param type to match
     * @param mapper row mapper
     * @return this
     */
    default This registerRowMapper(Type type, RowMapper<?> mapper) {
        return configure(RowMappers.class, c -> c.register(type, mapper));
    }

    /**
     * Convenience method for {@code getConfig(RowMappers.class).register(factory)}
     *
     * @param factory row mapper factory
     * @return this
     */
    default This registerRowMapper(RowMapperFactory factory) {
        return configure(RowMappers.class, c -> c.register(factory));
    }
}
