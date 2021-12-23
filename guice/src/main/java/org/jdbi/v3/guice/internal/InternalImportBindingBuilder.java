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

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Import binding builder.
 */
public final class InternalImportBindingBuilder<T> implements ScopedBindingBuilder {

    private final Key<T> concreteType;
    private final ScopedBindingBuilder binder;
    private final InternalBindingProvider<T> provider;

    public InternalImportBindingBuilder(LinkedBindingBuilder<T> binder, Key<T> concreteType) {
        checkNotNull(binder, "binder is null");

        this.concreteType = checkNotNull(concreteType, "concreteType is null");

        this.provider = new InternalBindingProvider<>(concreteType);
        this.binder = binder.toProvider(provider);
    }

    /**
     * Bind a different type as the given binding. This allows binding e.g. implementations to interface types.
     */
    public ScopedBindingBuilder to(Class<? extends T> clazz) {
        checkNotNull(clazz, "clazz is null");

        this.provider.setKey(concreteType.ofType(clazz));

        return this;
    }

    /**
     * Bind a different type as the given binding. This allows binding e.g. implementations to interface types.
     */
    public ScopedBindingBuilder to(TypeLiteral<? extends T> type) {
        checkNotNull(type, "type is null");

        this.provider.setKey(concreteType.ofType(type));

        return this;
    }

    @Override
    public void in(Class<? extends Annotation> scopeAnnotation) {
        checkNotNull(scopeAnnotation, "scopeAnnotation is null");
        this.binder.in(scopeAnnotation);
    }

    @Override
    public void in(Scope scope) {
        checkNotNull(scope, "scope is null");
        this.binder.in(scope);
    }

    @Override
    public void asEagerSingleton() {
        this.binder.asEagerSingleton();
    }

    static final class InternalBindingProvider<T> implements Provider<T> {

        private Key<? extends T> key;
        private Injector injector;

        InternalBindingProvider(Key<? extends T> key) {
            this.key = checkNotNull(key, "key is null");
        }

        InternalBindingProvider<T> setKey(Key<? extends T> key) {
            this.key = checkNotNull(key, "key is null");
            return this;
        }

        @Inject
        void setInjector(final Injector injector) {
            checkNotNull(injector, "injector is null");
            checkState(this.injector == null, "setInjector() called multiple times!");

            this.injector = injector;
        }

        @Override
        @CheckForNull
        public T get() {
            checkState(this.injector != null, "calling get() before setInjector()!");
            return injector.getInstance(key);
        }
    }
}
