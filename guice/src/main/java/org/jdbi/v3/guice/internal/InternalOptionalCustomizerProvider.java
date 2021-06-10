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

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import org.jdbi.v3.guice.AbstractJdbiConfigurationModule;
import org.jdbi.v3.guice.AbstractJdbiDefinitionModule;
import org.jdbi.v3.guice.GuiceJdbiCustomizer;

/**
 * Provider that looks up a guice customizer binding with a specific annotation. If the binding exists, return it when the provider is invoked, otherwise return
 * a dummy (empty) customizer.
 * <p>
 * This allows binding an optional "global" customizer if it exists and does not fail if the binding does not exist.
 * <p>
 * As the "global" customizer only exists if at least one module that extends {@link AbstractJdbiConfigurationModule} has been installed, this
 * provides a fallback and avoids binding failures in {@link AbstractJdbiDefinitionModule}.
 */
public class InternalOptionalCustomizerProvider implements Provider<GuiceJdbiCustomizer> {

    private final Class<? extends Annotation> annotation;

    private Injector optionalInjector = null;

    public InternalOptionalCustomizerProvider(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    @Override
    public GuiceJdbiCustomizer get() {
        return optionalInjector.getInstance(GuiceJdbiCustomizer.class);
    }

    @Inject
    void setInjector(final Injector injector) {
        this.optionalInjector = injector.createChildInjector(binder -> {
            final Key<GuiceJdbiCustomizer> customizerKey = Key.get(GuiceJdbiCustomizer.class, annotation);

            if (injector.getExistingBinding(customizerKey) == null) {
                binder.bind(customizerKey).toInstance(jdbi -> {});
            }

            binder.bind(GuiceJdbiCustomizer.class).to(customizerKey).in(Scopes.SINGLETON);
        });
    }
}
