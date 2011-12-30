package org.skife.jdbi.v2;

import java.util.ArrayList;
import java.util.List;

class ListContainerBuilder implements ContainerBuilder<List<?>>
{
    private final ArrayList<Object> list;

    ListContainerBuilder()
    {
        this.list = new ArrayList<Object>();
    }

    @Override
    public ListContainerBuilder add(Object it)
    {
        list.add(it);
        return this;
    }

    @Override
    public List<?> build()
    {
        return list;
    }
}
