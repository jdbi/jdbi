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

import com.google.inject.AbstractModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guice.internal.InternalGlobalJdbiModule;
import org.jdbi.v3.guice.internal.InternalJdbiBinder;
import org.jdbi.v3.guice.internal.JdbiGlobal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base module class for element configuration modules.
 * <p>
 * In more complex projects with multiple {@link Jdbi} definitions, there are often elements (e.g. mappers or plugins) that should be shared between all
 * instances.
 * <p>
 * Modules that extend {@link AbstractJdbiConfigurationModule} bind Jdbi related elements such as mappers, plugins, codecs etc. which then are used by Jdbi
 * definition modules (which extend {@link AbstractJdbiDefinitionModule}). Any binding that is defined here will be added to any Jdbi definition as long as they
 * either use the same configuration annotation. By default (unless using the constructors that explicitly take an annotation), all Jdbi definitions have all
 * configurations assigned.
 * <p>
 * This module does neither define a Jdbi binding nor requires a data source to be bound. Jdbi instances are defined in modules extending {@link
 * AbstractJdbiDefinitionModule} (which are private guice modules).
 */
public abstract class AbstractJdbiConfigurationModule extends AbstractModule implements JdbiBinder {

    private InternalJdbiBinder jdbiBinder;
    private final Class<? extends Annotation> annotationClass;

    /**
     * Creates an element configuration module.
     * <p>
     * All elements defined in this module will be bound using the {@link JdbiGlobal} annotation. This is the used by {@link AbstractJdbiDefinitionModule}
     * instances unless a custom annotation is used.
     */
    public AbstractJdbiConfigurationModule() {
        this(JdbiGlobal.class);
    }

    /**
     * Creates an element configuration module with a custom annotation.
     * <p>
     * All elements defined in this module will be bound using the given custom annotation. This allows creating multiple sets of element configuration modules
     * which can be referenced from modules extending the  {@link AbstractJdbiDefinitionModule} base class if a the same custom annotation is used.
     */
    public AbstractJdbiConfigurationModule(Class<? extends Annotation> annotationClass) {
        this.annotationClass = checkNotNull(annotationClass, "annotationClass is null");
    }

    @Override
    protected final void configure() {
        jdbiBinder = InternalJdbiBinder.jdbiBinder(binder(), annotationClass);

        try {
            install(InternalGlobalJdbiModule.forAnnotation(annotationClass));

            configureJdbi();
        } finally {
            jdbiBinder = null;
        }
    }

    public abstract void configureJdbi();

    /**
     * Provides access to the {@link JdbiBinder} instance.
     * <p>
     * Only valid when called from {@link AbstractJdbiConfigurationModule#configureJdbi}.
     */
    @Override
    public final JdbiBinder jdbiBinder() {
        checkState(jdbiBinder != null, "The jdbiBinder can only be used inside configureJdbi()");
        return jdbiBinder;
    }
}
