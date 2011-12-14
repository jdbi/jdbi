package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ContainerFactory;

class UnwrappedSingleValueFactory implements ContainerFactory<Object>
{
    public boolean accepts(Class<?> type)
    {
        return UnwrappedSingleValue.class.equals(type);
    }

    public ContainerBuilder newContainerBuilderFor(Class<?> type)
    {
        return new ContainerBuilder<Object>()
        {
            private Object it;

            public ContainerBuilder add(Object it)
            {
                this.it = it;
                return this;
            }

            public Object build()
            {
                return it;
            }
        };
    }

}
