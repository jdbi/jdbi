package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class ContainerFactoryRegistry
{
    private final Map<Class<?>, ContainerFactory<?>> cache     = new ConcurrentHashMap<Class<?>, ContainerFactory<?>>();
    private final List<ContainerFactory>             factories = new CopyOnWriteArrayList<ContainerFactory>();

    ContainerFactoryRegistry()
    {
        factories.add(new UnwrappedSingleValueFactory());
        factories.add(new ListContainerFactory());
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

        for (ContainerFactory factory : factories) {
            if (factory.accepts(type)) {
                cache.put(type, factory);
                return factory.newContainerBuilderFor(type);
            }
        }
        throw new IllegalStateException("No container builder available for " + type.getName());
    }

    public static class ListContainerFactory implements ContainerFactory<List>
    {

        public boolean accepts(Class<?> type)
        {
            return type.equals(List.class);
        }

        public ContainerBuilder<List> newContainerBuilderFor(Class<?> type)
        {
            return new ListContainerBuilder();
        }

    }
}
