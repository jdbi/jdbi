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

import javax.sql.DataSource;

import jakarta.annotation.Nullable;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guice.internal.InternalGuiceJdbiCustomizer;
import org.jdbi.v3.guice.internal.InternalGuiceJdbiFactory;
import org.jdbi.v3.guice.internal.InternalImportBindingBuilder;
import org.jdbi.v3.guice.internal.InternalJdbiBinder;
import org.jdbi.v3.guice.internal.InternalLooseImportBindingBuilder;
import org.jdbi.v3.guice.internal.JdbiGlobal;
import org.jdbi.v3.guice.internal.JdbiInternal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.jdbi.v3.guice.internal.InternalLooseImportBindingBuilder.createLooseBinding;

/**
 * Base module class to define {@link Jdbi} instances. This is a {@link PrivateModule} which will by default only expose the constructed {@link Jdbi} object as
 * a binding. Everything configured within this module is private to the module and not exposed. This allows installation of multiple instances of modules that
 * extend the {@link AbstractJdbiDefinitionModule} base class.
 * <p>
 * Each module requires a {@link DataSource} bound using the same annotation or annotation class as used on the module constructor. If this data source is not
 * present, the injection process will fail.
 * <p>
 * Successful binding will expose a {@link Jdbi} binding annotated with the same annotation or annotation class as used on the module constructor. Additional
 * elements defined in this module can be exposed using the {@link #exposeBinding(Class)} is called within the {@link #configureJdbi()} method.
 */
public abstract class AbstractJdbiDefinitionModule extends PrivateModule implements JdbiBinder {

    private final Annotation annotation;
    private final Class<? extends Annotation> annotationClass;
    private final Class<? extends Annotation> globalAnnotationClass;

    private InternalJdbiBinder jdbiBinder;

    /**
     * Create a Jdbi definition module.
     * <p>
     * A Jdbi definition module that uses this constructor will use the default element configuration from {@link AbstractJdbiConfigurationModule}s that do not
     * use any custom annotation.
     *
     * @param annotation The resulting {@link Jdbi} instance will be exposed using this annotation.
     */
    public AbstractJdbiDefinitionModule(Annotation annotation) {
        this(checkNotNull(annotation, "annotation is null"), null, JdbiGlobal.class);
    }

    /**
     * Create a Jdbi definition module with a custom annotation for element configuration.
     * <p>
     * A Jdbi definition module that uses this constructor will use the element configuration from {@link AbstractJdbiConfigurationModule}s that use the same
     * annotation as this constructor.
     *
     * @param annotation            The resulting {@link Jdbi} instance will be exposed using this annotation.
     * @param globalAnnotationClass Custom annotation class used to look up global settings for this {@link Jdbi} instance.
     */
    public AbstractJdbiDefinitionModule(Annotation annotation, Class<? extends Annotation> globalAnnotationClass) {
        this(checkNotNull(annotation, "annotation is null"), null, globalAnnotationClass);
    }

    /**
     * Create a Jdbi definition module.
     * <p>
     * A Jdbi definition module that uses this constructor will use the default element configuration from {@link AbstractJdbiConfigurationModule}s that do not
     * use any custom annotation.
     *
     * @param annotationClass The resulting {@link Jdbi} instance will be exposed using this annotation class.
     */
    public AbstractJdbiDefinitionModule(Class<? extends Annotation> annotationClass) {
        this(null, checkNotNull(annotationClass, "annotationClass is null"), JdbiGlobal.class);
    }

    /**
     * Create a Jdbi definition module with a custom annotation for element configuration.
     * <p>
     * A Jdbi definition module that uses this constructor will use the element configuration from {@link AbstractJdbiConfigurationModule}s that use the same
     * annotation as this constructor.
     *
     * @param annotationClass       The resulting {@link Jdbi} instance will be exposed using this annotation.
     * @param globalAnnotationClass Custom annotation class used to look up global settings for this {@link Jdbi} instance.
     */
    public AbstractJdbiDefinitionModule(Class<? extends Annotation> annotationClass, Class<? extends Annotation> globalAnnotationClass) {
        this(null, checkNotNull(annotationClass, "annotationClass is null"), globalAnnotationClass);
    }

    private AbstractJdbiDefinitionModule(@Nullable Annotation annotation,
        @Nullable Class<? extends Annotation> annotationClass,
        @Nullable Class<? extends Annotation> globalAnnotationClass) {
        this.annotation = annotation;
        this.annotationClass = annotationClass;
        this.globalAnnotationClass = globalAnnotationClass;
    }

    @Override
    protected final void configure() {

        this.jdbiBinder = InternalJdbiBinder.jdbiBinder(binder());

        try {
            // add the customizers for the global and module scope. Order is important here! As the customizers are executed in the order
            // of adding to the multibinder and Jdbi has a "last one wins" policy, the local customizer must be added *after* the global
            // customizer
            Multibinder<GuiceJdbiCustomizer> customizers = Multibinder.newSetBinder(binder(), GuiceJdbiCustomizer.class, JdbiInternal.class);

            if (globalAnnotationClass != null) {
                // binding for optional global settings; this provider returns it if it exists, otherwise just an empty dummy customizer.
                createLooseBinding(customizers.addBinding(), Key.get(GuiceJdbiCustomizer.class, globalAnnotationClass))
                    .withDefault(GuiceJdbiCustomizer.NOP)
                    .in(Scopes.SINGLETON);
            }

            customizers.addBinding().to(InternalGuiceJdbiCustomizer.class).in(Scopes.SINGLETON);

            // bring externally bound data source into the module namespace
            importBinding(DataSource.class).in(Scopes.SINGLETON);

            bind(Jdbi.class).toProvider(InternalGuiceJdbiFactory.class).in(Scopes.SINGLETON);

            // expose locally created Jdbi to the global namespace
            exposeBinding(Jdbi.class);

            configureJdbi();
        } finally {
            this.jdbiBinder = null;
        }
    }

    /**
     * Override this method to configure all aspects of a Jdbi instance.
     * <pre>
     * &#64;Override
     * protected void configureJdbi() {
     *   bindPlugin().toInstance(...);
     *   bindTransformer().to(...);
     *   bindRowMapper().to(...);
     * }
     * </pre>
     */
    public abstract void configureJdbi();

    /**
     * Provides access to the {@link JdbiBinder} instance.
     * <p>
     * Only valid when called from {@link AbstractJdbiDefinitionModule#configureJdbi}.
     */
    @Override
    public final JdbiBinder jdbiBinder() {
        checkState(jdbiBinder != null, "The jdbiBinder can only be used inside configureJdbi()");

        return jdbiBinder;
    }

    /**
     * Pulls an outside binding into the module scope if it exists. If it does not exist, bind a null value or a default. An outside binding uses the same
     * annotation or annotation class as the module.
     */
    public final <T> InternalLooseImportBindingBuilder<T> importBindingLoosely(TypeLiteral<T> type) {
        checkNotNull(type, "type is null");

        return createLooseBinding(binder().bind(type), createKey(type));
    }

    /**
     * Pulls an outside binding into the module scope if it exists. If it does not exist, bind a null value or a default. An outside binding uses the same
     * annotation or annotation class as the module.
     */
    public final <T> InternalLooseImportBindingBuilder<T> importBindingLoosely(Class<T> clazz) {
        checkNotNull(clazz, "clazz is null");

        return createLooseBinding(binder().bind(clazz), createKey(clazz));
    }

    /**
     * Pulls an outside binding into the module scope if it exists using the binder given. If it does not exist, bind a null value or a default. An outside
     * binding uses the same annotation or annotation class as the module.
     */
    public final <T> InternalLooseImportBindingBuilder<T> importBindingLoosely(LinkedBindingBuilder<T> binder, TypeLiteral<T> type) {
        checkNotNull(binder, "binder is null");
        checkNotNull(type, "type is null");

        return createLooseBinding(binder, createKey(type));
    }

    /**
     * Pulls an outside binding into the module scope if it exists using the binder given. If it does not exist, bind a null value or a default. An outside
     * binding uses the same annotation or annotation class as the module.
     */
    public final <T> InternalLooseImportBindingBuilder<T> importBindingLoosely(LinkedBindingBuilder<T> binder, Class<T> clazz) {
        checkNotNull(binder, "binder is null");
        checkNotNull(clazz, "clazz is null");

        return createLooseBinding(binder, createKey(clazz));
    }

    /**
     * Pulls an existing outside binding into the module scope. An outside binding uses the same annotation or annotation class as the module.
     */
    public final <T> InternalImportBindingBuilder<T> importBinding(Class<T> clazz) {
        checkNotNull(clazz, "clazz is null");

        return new InternalImportBindingBuilder<>(binder().bind(clazz), createKey(clazz));
    }

    /**
     * Pulls an existing outside binding into the module scope. An outside binding uses the same annotation or annotation class as the module.
     */
    public final <T> InternalImportBindingBuilder<T> importBinding(TypeLiteral<T> type) {
        checkNotNull(type, "type is null");

        return new InternalImportBindingBuilder<>(binder().bind(type), createKey(type));
    }

    /**
     * Pulls an existing outside binding into the module scope using the specified binder. An outside binding uses the same annotation or annotation class as
     * the module.
     */
    public final <T> InternalImportBindingBuilder<T> importBinding(LinkedBindingBuilder<T> binder, TypeLiteral<T> type) {
        checkNotNull(binder, "binder is null");
        checkNotNull(type, "type is null");

        return new InternalImportBindingBuilder<>(binder, createKey(type));
    }

    /**
     * Pulls an existing outside binding into the module scope using the specified binder. An outside binding uses the same annotation or annotation class as
     * the module.
     */
    public final <T> InternalImportBindingBuilder<T> importBinding(LinkedBindingBuilder<T> binder, Class<T> clazz) {
        checkNotNull(binder, "binder is null");
        checkNotNull(clazz, "clazz is null");

        return new InternalImportBindingBuilder<>(binder, createKey(clazz));
    }

    /**
     * Creates a {@link Key} object for a class that uses the annotation or annotation class used to construct this module.
     */
    public final <T> Key<T> createKey(Class<T> clazz) {
        checkNotNull(clazz, "clazz is null");
        return createKey(TypeLiteral.get(clazz));
    }

    /**
     * Creates a {@link Key} object for a {@link TypeLiteral} that uses the annotation or annotation class used to construct this module.
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    public final <T> Key<T> createKey(TypeLiteral<T> type) {
        checkNotNull(type, "type is null");

        if (annotation != null) {
            return Key.get(type, annotation);
        } else if (annotationClass != null) {
            return Key.get(type, annotationClass);
        } else {
            throw new IllegalStateException("Neither annotation or annotation class found!");
        }
    }

    /**
     * Exposes a binding that is in module scope (without annotations) and binds it using either the annotation or annotation class and then exposes it outside
     * the module.
     */
    public final <T> void exposeBinding(Class<T> clazz) {
        checkNotNull(clazz, "clazz is null");

        exposeBinding(TypeLiteral.get(clazz));
    }

    /**
     * Exposes a binding that is in module scope (without annotations) and binds it using either the annotation or annotation class and then exposes it outside
     * the module.
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    public final <T> void exposeBinding(TypeLiteral<T> type) {
        checkNotNull(type, "type is null");

        Key<T> key = createKey(type);
        bind(key).to(type).in(Scopes.SINGLETON);
        expose(key);
    }
}
