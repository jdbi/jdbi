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

        public boolean accepts(Class<?> type)
        {
            return type.equals(SortedSet.class);
        }

        public ContainerBuilder<SortedSet<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<SortedSet<?>>()
            {
                private SortedSet<Object> s = new TreeSet<Object>();

                public ContainerBuilder<SortedSet<?>> add(Object it)
                {
                    s.add(it);
                    return this;
                }

                public SortedSet<?> build()
                {
                    return s;
                }
            };
        }
    }

    static class SetContainerFactory implements ContainerFactory<Set<?>>
    {

        public boolean accepts(Class<?> type)
        {
            return Set.class.equals(type) || LinkedHashSet.class.equals(type);
        }

        public ContainerBuilder<Set<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Set<?>>()
            {
                private Set<Object> s = new LinkedHashSet<Object>();

                public ContainerBuilder<Set<?>> add(Object it)
                {
                    s.add(it);
                    return this;
                }

                public Set<?> build()
                {
                    return s;
                }
            };
        }
    }

    static class ListContainerFactory implements ContainerFactory<List<?>>
    {

        public boolean accepts(Class<?> type)
        {
            return type.equals(List.class) || type.equals(Collection.class) || type.equals(Iterable.class);
        }

        public ContainerBuilder<List<?>> newContainerBuilderFor(Class<?> type)
        {
            return new ListContainerBuilder();
        }

    }
}
