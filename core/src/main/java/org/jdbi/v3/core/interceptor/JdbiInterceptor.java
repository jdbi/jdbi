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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.jdbi.v3.meta.Alpha;

/**
 * Generic interface to allow transformation operation interception. Used to manage the various inferred data types that may need special treatment.
 *
 * @param <S> Transformation source type.
 * @param <T> Type of the transformation result.
 */
@Alpha
@FunctionalInterface
public interface JdbiInterceptor<S, T> {

    /**
     * Process a given source object.
     * <ul>
     *     <li>If the interceptor wants to process the object, return the result directly.</li>
     *     <li>If the interceptor passes on processing, it must return {@link JdbiInterceptionChain#next()}</li>
     * </ul>
     *
     * <pre>{@code
     *  class SomeInterceptor implements JdbiInterceptor<Foo, Bar> {
     *      @Override
     *      public Bar intercept(Foo source, JdbiInterceptionChain<Foo, Bar> chain) {
     *          if (source != null && source.isBarCompatible()) {
     *              return new Bar(source);
     *          } else {
     *              return chain.next();
     *          }
     *      }
     *  }
     * }</pre>
     *
     * @param source A source object.
     * @return The destination type.
     */
    @CheckForNull
    T intercept(@Nullable S source, JdbiInterceptionChain<T> chain);
}
