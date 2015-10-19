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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;

/**
 * Factor for building {@link Binder} instances. This interface is used by
 * the {@link BindingAnnotation}
 */
public interface BinderFactory<T extends Annotation, O>
{
    /**
     * Called to build a Binder
     * @param annotation the {@link BindingAnnotation} which lead to this call
     * @return a binder to use
     */
    Binder<T, O> build(T annotation);
}
