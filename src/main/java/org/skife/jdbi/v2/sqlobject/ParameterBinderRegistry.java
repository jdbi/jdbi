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
package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class ParameterBinderRegistry
{
    static final ParameterBinderFactory DEFAULT_BINDER_FACTORY = new ParameterBinderFactory() {
        @Override
        public boolean accepts(Class type, Method method, int param_idx) {
            return true;
        }

        @Override
        public Binder binderFor(Class type, Method method, int param_idx) {
            return new PositionalBinder(param_idx);
        }
    };

    private final List<ParameterBinderFactory> orderedBinderFactories = new CopyOnWriteArrayList<ParameterBinderFactory>();
    private final ConcurrentHashMap<ClassMethodParameter, ParameterBinderFactory> factoryCache = new ConcurrentHashMap<ClassMethodParameter, ParameterBinderFactory>();

    private static class ClassMethodParameter {
        final Class type;
        final Method method;
        final int param_idx;

        ClassMethodParameter(Class type, Method method, int param_idx) {
            this.type = type;
            this.method = method;
            this.param_idx = param_idx;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (obj instanceof ClassMethodParameter) {
                ClassMethodParameter other = (ClassMethodParameter)obj;
                return (other.type.equals(type) && other.method.equals(method) && other.param_idx == param_idx);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return type.hashCode() + method.hashCode() + param_idx;
        }
    }

    /**
     * Copy Constructor
     */
    ParameterBinderRegistry(ParameterBinderRegistry parent)
    {
        orderedBinderFactories.addAll(parent.orderedBinderFactories);
        factoryCache.putAll(parent.factoryCache);
    }

    ParameterBinderRegistry() {

    }

    /**
     * Add factory to the front of the list, so it has precedence over previously added
     * @param factory
     */
    public void addFactoryAsFirst(ParameterBinderFactory factory) {
        orderedBinderFactories.add(0, factory);
        factoryCache.clear();
    }

    /**
     * Add factory to the end of the list, so it has lowest precedence other than for the DEFAULT_BINDER_FACTORY which is always last
     * @param factory
     */
    public void addFactoryAsLast(ParameterBinderFactory factory) {
        orderedBinderFactories.add(factory);
        factoryCache.clear();
    }

    /**
     * Clear all registered binder factories, other than DEFAULT_BINDER_FACTORY which is always implied as lowest precedence
     */
    public void reset() {
        orderedBinderFactories.clear();
                factoryCache.clear();
    }

    public Binder binderFor(Class type, Method method, int param_idx) {
        final ClassMethodParameter cacheKey = new ClassMethodParameter(type, method, param_idx);
        final ParameterBinderFactory cached = factoryCache.get(cacheKey);
        if (cached != null) {
            return cached.binderFor(type, method, param_idx);
        }

        for (ParameterBinderFactory factory : orderedBinderFactories) {
            if (factory.accepts(type, method, param_idx)) {
                Binder binder = factory.binderFor(type, method, param_idx);
                if (binder != null) {
                    factoryCache.put(cacheKey, factory);
                    return factory.binderFor(type, method, param_idx);
                }
            }
        }

        factoryCache.put(cacheKey, DEFAULT_BINDER_FACTORY);
        return DEFAULT_BINDER_FACTORY.binderFor(type, method, param_idx);
    }
}
