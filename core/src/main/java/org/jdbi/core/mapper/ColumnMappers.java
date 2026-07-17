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
package org.jdbi.core.mapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.core.array.SqlArrayMapperFactory;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.enums.internal.EnumMapperFactory;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.meta.Alpha;

/**
 * Registry of {@link ColumnMapperFactory} instances. Holds only registration data and the
 * null-primitive coalescing policy; resolving a factory into a {@link ColumnMapper} for a given type
 * (and caching the result) is done per configuration registry by {@link MapperResolver}.
 */
public class ColumnMappers implements JdbiConfig<ColumnMappers> {

    private final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors;

    private final List<QualifiedColumnMapperFactory> factories;

    private boolean coalesceNullPrimitivesToDefaults = true;

    public ColumnMappers() {
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(InferredColumnMapperFactory::new);
        factories = new CopyOnWriteArrayList<>();
        register(new SqlArrayMapperFactory());
        register(new JavaTimeMapperFactory());
        register(new SqlTimeMapperFactory());
        register(new InternetMapperFactory());
        register(new EssentialsMapperFactory());
        register(new BoxedMapperFactory());
        register(new PrimitiveMapperFactory());
        register(new OptionalColumnMapperFactory());
        register(new EnumMapperFactory());
        register(new NVarcharMapper());
    }

    private ColumnMappers(ColumnMappers that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(that.inferenceInterceptors);
        coalesceNullPrimitivesToDefaults = that.coalesceNullPrimitivesToDefaults;
    }

    /**
     * Returns the {@link JdbiInterceptionChainHolder} for the ColumnMapper inference. This chain allows registration of custom interceptors to change the standard
     * type inference for the {@link ColumnMappers#register(ColumnMapper)} method.
     */
    @Alpha
    public JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> getInferenceInterceptors() {
        return inferenceInterceptors;
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     * <p>
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the column mapper
     * @return this
     * @throws UnsupportedOperationException if the ColumnMapper is not a concretely parameterized type
     */
    public ColumnMappers register(ColumnMapper<?> mapper) {
        QualifiedColumnMapperFactory factory = inferenceInterceptors.process(mapper);

        return this.register(factory);
    }

    /**
     * Register a column mapper for a given explicit {@link GenericType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param <T> the type
     * @param type the generic type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public <T> ColumnMappers register(GenericType<T> type, ColumnMapper<T> mapper) {
        return this.register(ColumnMapperFactory.of(type.getType(), mapper));
    }

    /**
     * Register a column mapper for a given explicit {@link Type}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public ColumnMappers register(Type type, ColumnMapper<?> mapper) {
        return this.register(ColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper for a given {@link QualifiedType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    public <T> ColumnMappers register(QualifiedType<T> type, ColumnMapper<T> mapper) {
        return this.register(QualifiedColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return this
     */
    public ColumnMappers register(ColumnMapperFactory factory) {
        return register(QualifiedColumnMapperFactory.adapt(factory));
    }

    /**
     * Register a qualified column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the qualified column mapper factory
     * @return this
     */
    public ColumnMappers register(QualifiedColumnMapperFactory factory) {
        factories.add(0, factory);
        return this;
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link MapperResolver}.
     *
     * @return the registered column mapper factories
     */
    List<QualifiedColumnMapperFactory> getFactories() {
        return factories;
    }

    /**
     * Returns true if database {@code NULL} values should be transformed to the default value for primitives.
     *
     * @return {@code true} if database {@code NULL}s should translate to the JDBC defaults for primitives, or throw an exception otherwise.
     *
     * Default value is true: nulls will be coalesced to defaults.
     */
    public boolean getCoalesceNullPrimitivesToDefaults() {
        return coalesceNullPrimitivesToDefaults;
    }

    /**
     * Use the JDBC default value for primitive types if a SQL NULL value was returned by the database.
     * If this property is set to {@code false}, Jdbi will throw an exception when trying to map a SQL {@code NULL} value to a primitive type.
     *
     * @param coalesceNullPrimitivesToDefaults If true, then use the JDBC default value, otherwise throw an exception.
     * @return this
     */
    public ColumnMappers setCoalesceNullPrimitivesToDefaults(boolean coalesceNullPrimitivesToDefaults) {
        this.coalesceNullPrimitivesToDefaults = coalesceNullPrimitivesToDefaults;
        return this;
    }

    @Override
    public ColumnMappers createCopy() {
        return new ColumnMappers(this);
    }
}
