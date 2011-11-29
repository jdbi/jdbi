package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.ContainerBuilder;

public interface ContainerFactory<T>
{
    public boolean accepts(Class<?> type);

    public ContainerBuilder<T> newContainerBuilderFor(Class<?> type);
}
