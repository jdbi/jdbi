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
package org.jdbi.v3.core.interceptor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;


import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.jdbi.v3.meta.Alpha;

import static java.util.Objects.requireNonNull;

/**
 * An interception chain holder to manage transformation operations.
 *
 * @param <S> Type of the transformation source.
 * @param <T> Type of the transformation target.
 */
@Alpha
public final class JdbiInterceptionChainHolder<S, T> {

    @SuppressWarnings("UnnecessaryLambda") // constant for readablity
    private static final Function<?, ?> DEFAULT_TRANSFORMER = source -> {
        if (source == null) {
            throw new UnsupportedOperationException("null value is not supported");
        } else {
            throw new UnsupportedOperationException("object type '" + source.getClass().getSimpleName() + "' is not supported");
        }
    };

    private final List<JdbiInterceptor<S, T>> interceptors;
    private final Function<S, T> defaultTransformer;

    /**
     * Creates a new chain holder with a default interceptor.
     *
     * @param defaultTransformer A default interceptor that is used when no other registered interceptor processes a source object. Must not be null.
     */
    public JdbiInterceptionChainHolder(Function<S, T> defaultTransformer) {
        interceptors = new CopyOnWriteArrayList<>();
        this.defaultTransformer = requireNonNull(defaultTransformer, "defaultTransformer is null");
    }

    /**
     * Creates a new chain holder. Throws {@link UnsupportedOperationException} if no registered interceptor processes a source object.
     */
    public JdbiInterceptionChainHolder() {
        interceptors = new CopyOnWriteArrayList<>();
        this.defaultTransformer = (Function<S, T>) DEFAULT_TRANSFORMER;
    }

    public JdbiInterceptionChainHolder(JdbiInterceptionChainHolder<S, T> that) {
        this.interceptors = new CopyOnWriteArrayList<>(that.interceptors);
        this.defaultTransformer = that.defaultTransformer;
    }

    /**
     * Processes a source object and returns a target object.
     *
     * @param source A source object.
     * @return A target object processed by one of the registered {@link JdbiInterceptor} instances.
     */
    @NonNull
    public T process(@Nullable S source) {
        ChainInstance instance = new ChainInstance(source);
        T result = instance.next();
        requireNonNull(result, "chain returned null value for '" + source + "'");
        return result;
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

    final class ChainInstance implements JdbiInterceptionChain<T> {

        private final Iterator<JdbiInterceptor<S, T>> iterator;
        private final S source;

        ChainInstance(S source) {
            this.source = source;
            this.iterator = interceptors.iterator();
        }

        @Override
        public T next() {
            if (iterator.hasNext()) {
                JdbiInterceptor<S, T> interceptor = iterator.next();
                return interceptor.intercept(source, this);
            } else {
                return defaultTransformer.apply(source);
            }
        }
    }
}
