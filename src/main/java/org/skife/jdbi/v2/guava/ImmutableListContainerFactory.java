package org.skife.jdbi.v2.guava;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.tweak.ContainerFactory;

public class ImmutableListContainerFactory implements ContainerFactory<ImmutableList>
{
    public boolean accepts(Class<?> type)
    {
        return ImmutableList.class.equals(type);
    }

    public ContainerBuilder<ImmutableList> newContainerBuilderFor(Class<?> type)
    {
        return new ContainerBuilder<ImmutableList>()
        {
            private final ImmutableList.Builder builder = ImmutableList.builder();

            public ContainerBuilder<ImmutableList> add(Object it)
            {
                builder.add(it);
                return this;
            }

            public ImmutableList build()
            {
                return builder.build();
            }
        };
    }
}
