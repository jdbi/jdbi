/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class ContainerFactoryRegistry
{
    private final Map<Class<?>, ContainerFactory<?>> cache     = new ConcurrentHashMap<Class<?>, ContainerFactory<?>>();
    private final List<ContainerFactory>             factories = new CopyOnWriteArrayList<ContainerFactory>();

    ContainerFactoryRegistry()
    {
        factories.add(new ListContainerFactory());
        factories.add(new SetContainerFactory());
        factories.add(new SortedSetContainerFactory());
        factories.add(new UnwrappedSingleValueFactory());
    }

    ContainerFactoryRegistry(ContainerFactoryRegistry parent)
    {
        cache.putAll(parent.cache);
        factories.addAll(parent.factories);
    }

    void register(ContainerFactory<?> factory)
    {
        factories.add(factory);
        cache.clear();
    }

    public ContainerFactoryRegistry createChild()
    {
        return new ContainerFactoryRegistry(this);
    }

    public ContainerBuilder createBuilderFor(Class<?> type)
    {
        if (cache.containsKey(type)) {
            return cache.get(type).newContainerBuilderFor(type);
        }


        for (int i = factories.size(); i > 0; i--) {
            ContainerFactory factory = factories.get(i - 1);
            if (factory.accepts(type)) {
                cache.put(type, factory);
                return factory.newContainerBuilderFor(type);
            }
        }

        throw new IllegalStateException("No container builder available for " + type.getName());
    }

    static class SortedSetContainerFactory implements ContainerFactory<SortedSet<?>> {

        @Override
        public boolean accepts(Class<?> type)
        {
            return type.equals(SortedSet.class);
        }

        @Override
        public ContainerBuilder<SortedSet<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<SortedSet<?>>()
            {
                private SortedSet<Object> s = new TreeSet<Object>();

                @Override
                public ContainerBuilder<SortedSet<?>> add(Object it)
                {
                    s.add(it);
                    return this;
                }

                @Override
                public SortedSet<?> build()
                {
                    return s;
                }
            };
        }
    }

    static class SetContainerFactory implements ContainerFactory<Set<?>>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return Set.class.equals(type) || LinkedHashSet.class.equals(type);
        }

        @Override
        public ContainerBuilder<Set<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Set<?>>()
            {
                private Set<Object> s = new LinkedHashSet<Object>();

                @Override
                public ContainerBuilder<Set<?>> add(Object it)
                {
                    s.add(it);
                    return this;
                }

                @Override
                public Set<?> build()
                {
                    return s;
                }
            };
        }
    }

    static class ListContainerFactory implements ContainerFactory<List<?>>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return type.equals(List.class) || type.equals(Collection.class) || type.equals(Iterable.class);
        }

        @Override
        public ContainerBuilder<List<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ListContainerBuilder();
        }

    }
}
