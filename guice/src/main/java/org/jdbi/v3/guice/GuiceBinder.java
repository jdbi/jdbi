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

import java.lang.reflect.Type;
import java.util.function.Function;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exposes binders for all aspects of JDBI objects.
 */
final class GuiceBinder {

    private final Multibinder<RowMapper<?>> rowMapperBinder;
    private final Multibinder<ColumnMapper<?>> columnMapperBinder;
    private final MapBinder<QualifiedType<?>, Codec<?>> codecBinder;
    private final MapBinder<Class<?>, String> arrayTypeBinder;
    private final Multibinder<JdbiPlugin> pluginBinder;
    private final Multibinder<Function<Jdbi, Jdbi>> transformerBinder;

    /**
     * Creates a new JDBI binder.
     */
    public static GuiceBinder jdbiBinder(Binder binder) {
        return new GuiceBinder(binder);
    }

    private GuiceBinder(Binder binder) {
        Binder b = checkNotNull(binder, "binder is null").skipSources(getClass());

        this.rowMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<RowMapper<?>>() {});
        this.columnMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<ColumnMapper<?>>() {});
        this.codecBinder = MapBinder.newMapBinder(b, new TypeLiteral<QualifiedType<?>>() {}, new TypeLiteral<Codec<?>>() {});
        this.arrayTypeBinder = MapBinder.newMapBinder(b, new TypeLiteral<Class<?>>() {}, new TypeLiteral<String>() {});
        this.pluginBinder = Multibinder.newSetBinder(b, new TypeLiteral<JdbiPlugin>() {});
        this.transformerBinder = Multibinder.newSetBinder(b, new TypeLiteral<Function<Jdbi, Jdbi>>() {});
    }

    /**
     * Creates a new binding for a {@link RowMapper}.
     *
     * <pre>
     *   GuiceBinder.jdbiBinder(binder).bindRowMapper().to(FancyDataTypeMapper.class).in(Scopes.SINGLETON);
     *   GuiceBinder.jdbiBinder(binder).bindRowMapper().toInstance(new BoringDataTypeMapper()).in(Scopes.SINGLETON);
     * </pre>
     */
    public LinkedBindingBuilder<RowMapper<?>> bindRowMapper() {
        return rowMapperBinder.addBinding();
    }

    /**
     * Creates a new binding for a {@link ColumnMapper}.
     */
    public LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper() {
        return columnMapperBinder.addBinding();
    }

    /**
     * Creates a new binding for a SQL array type.
     */
    public LinkedBindingBuilder<String> bindArrayType(Class<?> arrayType) {
        checkNotNull(arrayType, "arrayType is null");
        return arrayTypeBinder.addBinding(arrayType);
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link QualifiedType}.
     */
    public LinkedBindingBuilder<Codec<?>> bindCodec(QualifiedType<?> qualifiedType) {
        checkNotNull(qualifiedType, "qualifiedType is null");
        return codecBinder.addBinding(qualifiedType);
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link GenericType}.
     */
    public LinkedBindingBuilder<Codec<?>> bindCodec(GenericType<?> genericType) {
        checkNotNull(genericType, "genericType is null");
        return codecBinder.addBinding(QualifiedType.of(genericType.getType()));
    }

    /**
     * Creates a new binding for a {@link Codec} using a {@link Type}.
     */
    public LinkedBindingBuilder<Codec<?>> bindCodec(Type type) {
        checkNotNull(type, "type is null");
        return codecBinder.addBinding(QualifiedType.of(type));
    }

    /**
     * Creates a new binding for a {@link JdbiPlugin}.
     */
    public LinkedBindingBuilder<JdbiPlugin> bindPlugin() {
        return pluginBinder.addBinding();
    }

    /**
     * Creates a new binding for a transformer function. All registered transformer functions are called on a newly created JDBI object and allow further
     * customization of all aspects of the JDBI object.
     */
    public LinkedBindingBuilder<Function<Jdbi, Jdbi>> bindTransformer() {
        return transformerBinder.addBinding();
    }
}
