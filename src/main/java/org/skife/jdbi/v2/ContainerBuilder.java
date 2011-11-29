package org.skife.jdbi.v2;

public interface ContainerBuilder<ContainerType>
{
    public ContainerBuilder<ContainerType> add(Object it);

    public ContainerType build();

}
