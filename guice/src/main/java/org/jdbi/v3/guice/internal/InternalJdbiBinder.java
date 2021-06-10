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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.guice.GuiceJdbiCustomizer;
import org.jdbi.v3.guice.JdbiBinder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exposes binders for all aspects of JDBI objects.
 */
public final class InternalJdbiBinder implements JdbiBinder {

    private final Multibinder<RowMapper<?>> rowMapperBinder;
    private final MapBinder<Type, RowMapper<?>> qualifiedRowMapperBinder;
    private final Multibinder<ColumnMapper<?>> columnMapperBinder;
    private final MapBinder<QualifiedType<?>, ColumnMapper<?>> qualifiedColumnMapperBinder;
    private final MapBinder<QualifiedType<?>, Codec<?>> codecBinder;
    private final MapBinder<Class<?>, String> arrayTypeBinder;
    private final Multibinder<JdbiPlugin> pluginBinder;
    private final Multibinder<GuiceJdbiCustomizer> customizerBinder;

    /**
     * Creates a new binder for Jdbi related elements.
     */
    public static InternalJdbiBinder jdbiBinder(Binder binder) {
        return new InternalJdbiBinder(binder);
    }

    public static InternalJdbiBinder jdbiBinder(Binder binder, Class<? extends Annotation> annotationClass) {
        return new InternalJdbiBinder(binder, annotationClass);
    }

    private InternalJdbiBinder(Binder binder) {
        Binder b = checkNotNull(binder, "binder is null").skipSources(getClass());

        this.rowMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<RowMapper<?>>() {});
        this.qualifiedRowMapperBinder = MapBinder.newMapBinder(b, new TypeLiteral<Type>() {}, new TypeLiteral<RowMapper<?>>() {});
        this.columnMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<ColumnMapper<?>>() {});
        this.qualifiedColumnMapperBinder = MapBinder.newMapBinder(b, new TypeLiteral<QualifiedType<?>>() {}, new TypeLiteral<ColumnMapper<?>>() {});
        this.codecBinder = MapBinder.newMapBinder(b, new TypeLiteral<QualifiedType<?>>() {}, new TypeLiteral<Codec<?>>() {});
        this.arrayTypeBinder = MapBinder.newMapBinder(b, new TypeLiteral<Class<?>>() {}, new TypeLiteral<String>() {});
        this.pluginBinder = Multibinder.newSetBinder(b, new TypeLiteral<JdbiPlugin>() {});
        this.customizerBinder = Multibinder.newSetBinder(b, new TypeLiteral<GuiceJdbiCustomizer>() {});
    }

    private InternalJdbiBinder(Binder binder, Class<? extends Annotation> a) {
        Binder b = checkNotNull(binder, "binder is null").skipSources(getClass());

        this.rowMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<RowMapper<?>>() {}, a);
        this.qualifiedRowMapperBinder = MapBinder.newMapBinder(b, new TypeLiteral<Type>() {}, new TypeLiteral<RowMapper<?>>() {}, a);
        this.columnMapperBinder = Multibinder.newSetBinder(b, new TypeLiteral<ColumnMapper<?>>() {}, a);
        this.qualifiedColumnMapperBinder = MapBinder.newMapBinder(b, new TypeLiteral<QualifiedType<?>>() {}, new TypeLiteral<ColumnMapper<?>>() {}, a);
        this.codecBinder = MapBinder.newMapBinder(b, new TypeLiteral<QualifiedType<?>>() {}, new TypeLiteral<Codec<?>>() {}, a);
        this.arrayTypeBinder = MapBinder.newMapBinder(b, new TypeLiteral<Class<?>>() {}, new TypeLiteral<String>() {}, a);
        this.pluginBinder = Multibinder.newSetBinder(b, new TypeLiteral<JdbiPlugin>() {}, a);
        this.customizerBinder = Multibinder.newSetBinder(b, new TypeLiteral<GuiceJdbiCustomizer>() {}, a);
    }

    @Override
    public LinkedBindingBuilder<RowMapper<?>> bindRowMapper() {
        return rowMapperBinder.addBinding();
    }

    @Override
    public LinkedBindingBuilder<RowMapper<?>> bindRowMapper(GenericType<?> genericType) {
        checkNotNull(genericType, "genericType is null");
        return qualifiedRowMapperBinder.addBinding(genericType.getType());
    }

    @Override
    public LinkedBindingBuilder<RowMapper<?>> bindRowMapper(Type type) {
        checkNotNull(type, "type is null");
        return qualifiedRowMapperBinder.addBinding(type);
    }

    @Override
    public LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper() {
        return columnMapperBinder.addBinding();
    }

    @Override
    public LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(QualifiedType<?> qualifiedType) {
        checkNotNull(qualifiedType, "qualifiedType is null");
        return qualifiedColumnMapperBinder.addBinding(qualifiedType);
    }

    @Override
    public LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(GenericType<?> genericType) {
        checkNotNull(genericType, "genericType is null");
        return qualifiedColumnMapperBinder.addBinding(QualifiedType.of(genericType.getType()));
    }

    @Override
    public LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper(Type type) {
        checkNotNull(type, "type is null");
        return qualifiedColumnMapperBinder.addBinding(QualifiedType.of(type));
    }

    @Override
    public LinkedBindingBuilder<String> bindArrayType(Class<?> arrayType) {
        checkNotNull(arrayType, "arrayType is null");
        return arrayTypeBinder.addBinding(arrayType);
    }

    @Override
    public LinkedBindingBuilder<Codec<?>> bindCodec(QualifiedType<?> qualifiedType) {
        checkNotNull(qualifiedType, "qualifiedType is null");
        return codecBinder.addBinding(qualifiedType);
    }

    @Override
    public LinkedBindingBuilder<Codec<?>> bindCodec(GenericType<?> genericType) {
        checkNotNull(genericType, "genericType is null");
        return codecBinder.addBinding(QualifiedType.of(genericType.getType()));
    }

    @Override
    public LinkedBindingBuilder<Codec<?>> bindCodec(Type type) {
        checkNotNull(type, "type is null");
        return codecBinder.addBinding(QualifiedType.of(type));
    }

    @Override
    public LinkedBindingBuilder<JdbiPlugin> bindPlugin() {
        return pluginBinder.addBinding();
    }

    @Override
    public LinkedBindingBuilder<GuiceJdbiCustomizer> bindCustomizer() {
        return customizerBinder.addBinding();
    }
}
