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
package org.jdbi.v3.core.inference;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * An interception chain to process transformation operations.
 *
 * @param <S> The source type.
 * @param <T> The target type.
 */
public final class JdbiInterceptorChain<S, T> {

    private final List<JdbiInterceptor<S, T>> interceptors = new CopyOnWriteArrayList<>();
    private JdbiInterceptor<S, T> defaultInterceptor;

    /**
     * Creates a new chain with a default interceptor.
     *
     * @param defaultInterceptor A default interceptor that is used when no other registered interceptor processes a source object. Must not be null.
     */
    public JdbiInterceptorChain(JdbiInterceptor<S, T> defaultInterceptor) {
        this.defaultInterceptor = requireNonNull(defaultInterceptor, "defaultInterceptor is null");
    }

    /**
     * Creates a new chain. Throws {@link UnsupportedOperationException} if no registered interceptor processes a source object.
     */
    public JdbiInterceptorChain() {
        this.defaultInterceptor = defaultInterceptor();
    }

    /**
     * Processes a source object and returns a target object.
     *
     * @param source A source object.
     * @return A target object processed by one of the registered {@link JdbiInterceptor} instances.
     */
    @Nonnull
    public T process(@Nullable S source) {
        for (JdbiInterceptor<S, T> interceptor : interceptors) {
            if (interceptor.accept(source)) {
                return requireNonNull(interceptor.intercept(source), "Could not determine result '" + source + "'");
            }
        }

        return requireNonNull(defaultInterceptor.intercept(source), "Could not determine result '" + source + "'");
    }

    /**
     * Registers a new interceptor at the beginning of the chain. Any subsequent call to {@link #process(Object)} will use this interceptor before all other
     * already registered interceptors.
     *
     * @param interceptor An object implementing {@link JdbiInterceptor}.
     */
    public void addFirst(JdbiInterceptor<S, T> interceptor) {
        requireNonNull(interceptor, "interceptor is null");

        interceptors.add(0, interceptor);
    }

    /**
     * Registers a new interceptor at the end of the chain. Any subsequent call to {@link #process(Object)} will use this interceptor after all other already
     * registered interceptors.
     *
     * @param interceptor An object implementing {@link JdbiInterceptor}.
     */
    public void addLast(JdbiInterceptor<S, T> interceptor) {
        requireNonNull(interceptor, "interceptor is null");

        interceptors.add(interceptor);
    }

    public void copy(JdbiInterceptorChain<S, T> chain) {
        requireNonNull(chain, "chain is null");
        interceptors.clear();
        interceptors.addAll(chain.interceptors);
        defaultInterceptor = chain.defaultInterceptor;
    }

    /**
     * An interceptor that accepts any object and throws an {@link UnsupportedOperationException}.
     */
    private JdbiInterceptor<S, T> defaultInterceptor() {
        return new JdbiInterceptor<S, T>() {
            @Override
            public T intercept(S source) {
                if (source == null) {
                    throw new UnsupportedOperationException("null value is not supported!");
                } else {
                    throw new UnsupportedOperationException("object type '" + source.getClass().getSimpleName() + "' is not supported");
                }
            }

            @Override
            public boolean accept(Object source) {
                return true;
            }
        };
    }
}
