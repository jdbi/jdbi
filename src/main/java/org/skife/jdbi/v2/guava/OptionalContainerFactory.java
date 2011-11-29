package org.skife.jdbi.v2.guava;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.tweak.ContainerFactory;

public class OptionalContainerFactory implements ContainerFactory<Optional<?>>
{
    public boolean accepts(Class<?> type)
    {
        return Optional.class.equals(type);
    }

    public ContainerBuilder<Optional<?>> newContainerBuilderFor(Class<?> type)
    {
        return new ContainerBuilder<Optional<?>>()
        {
            private Object it = null;

            public ContainerBuilder<Optional<?>> add(Object it)
            {
                this.it = it;
                return this;
            }

            public Optional<?> build()
            {
                return Optional.fromNullable(it);
            }
        };
    }

}
