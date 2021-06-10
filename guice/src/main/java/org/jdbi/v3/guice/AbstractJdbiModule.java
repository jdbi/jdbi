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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

import javax.sql.DataSource;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base module for JDBI configuration. This is a {@link PrivateModule} which will only expose the constructed {@link Jdbi} object to the rest of the Guice
 * modules. Everything configured within this module is private to the module and not exposed.
 * <p>
 * This allows installation of multiple instances of modules that extend the {@link AbstractJdbiModule}.
 * <p>
 * The module will bind a {@link DataSource} using the same annotation or annotation class as used on the module constructor and exposes a {@link Jdbi} object
 * annotated with the same annotation or annotation class.
 */
public abstract class AbstractJdbiModule extends PrivateModule {

    private final Annotation annotation;
    private final Class<? extends Annotation> annotationClass;

    private GuiceBinder jdbiBinder;

    /**
     * Creates a new module using an {@link Annotation} object.
     */
    public AbstractJdbiModule(Annotation annotation) {
        this.annotation = checkNotNull(annotation, "annotation is null");
        this.annotationClass = null;
    }

    /**
     * Creates a new module using an {@link Class} instance representing an annotation.
     */
    public AbstractJdbiModule(Class<? extends Annotation> annotationClass) {
        this.annotationClass = checkNotNull(annotationClass, "annotationClass is null");
        this.annotation = null;
    }

    @Override
    protected void configure() {
        bind(GuiceFactory.class).in(Scopes.SINGLETON);

        jdbiBinder = GuiceBinder.jdbiBinder(binder());

        if (annotation != null) {
            bind(DataSource.class).to(Key.get(DataSource.class, annotation)).in(Scopes.SINGLETON);

            bind(Jdbi.class).annotatedWith(annotation).to(Jdbi.class).in(Scopes.SINGLETON);
            expose(Jdbi.class).annotatedWith(annotation);

        }

        if (annotationClass != null) {
            bind(DataSource.class).to(Key.get(DataSource.class, annotationClass)).in(Scopes.SINGLETON);

            bind(Jdbi.class).annotatedWith(annotationClass).to(Jdbi.class).in(Scopes.SINGLETON);
            expose(Jdbi.class).annotatedWith(annotationClass);
        }

        try {
            configureJdbi();
        } finally {
            jdbiBinder = null;
        }
    }

    @Provides
    @Singleton
    Jdbi getJdbi(GuiceFactory guiceFactory, DataSource dataSource) {
        return guiceFactory.create(dataSource);
    }

    /**
     * Override this method to configure all aspects of a JDBI.
     * <pre>
     * &#64;Override
     * protected void configureJdbi() {
     *   bindPlugin().toInstance(...);
     *   bindTransformer().to(...);
     *   bindRowMapper().to(...);
     * }
     * </pre>
     */
    protected abstract void configureJdbi();

    /**
     * Provides access to the {@link GuiceBinder} instance.
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi}.
     */
    protected GuiceBinder jdbiBinder() {
        checkState(jdbiBinder != null, "The jdbiBinder can only be used inside configureJdbi()");
        return jdbiBinder;
    }

    /**
     * @see GuiceBinder#bindRowMapper()
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<RowMapper<?>> bindRowMapper() {
        return jdbiBinder().bindRowMapper();
    }

    /**
     * @see GuiceBinder#bindColumnMapper()
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<ColumnMapper<?>> bindColumnMapper() {
        return jdbiBinder().bindColumnMapper();
    }

    /**
     * @see GuiceBinder#bindCodec(QualifiedType)
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<Codec<?>> bindCodec(QualifiedType<?> qualifiedType) {
        return jdbiBinder().bindCodec(qualifiedType);
    }

    /**
     * @see GuiceBinder#bindCodec(GenericType)
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<Codec<?>> bindCodec(GenericType<?> genericType) {
        return jdbiBinder().bindCodec(genericType);
    }

    /**
     * @see GuiceBinder#bindCodec(Type)
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<Codec<?>> bindCodec(Type type) {
        return jdbiBinder().bindCodec(type);
    }

    /**
     * @see GuiceBinder#bindArrayType(Class)
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<String> bindArrayType(Class<?> arrayType) {
        return jdbiBinder().bindArrayType(arrayType);
    }

    /**
     * @see GuiceBinder#bindPlugin()
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<JdbiPlugin> bindPlugin() {
        return jdbiBinder().bindPlugin();
    }

    /**
     * @see GuiceBinder#bindTransformer()
     * <p>
     * Only valid when called from {@link AbstractJdbiModule#configureJdbi()}.
     */
    protected LinkedBindingBuilder<Function<Jdbi, Jdbi>> bindTransformer() {
        return jdbiBinder().bindTransformer();
    }
}
