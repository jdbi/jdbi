package org.skife.jdbi.v2.guava;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.util.List;

public class ImmutableListContainerFactory implements ContainerFactory<ImmutableList>
{
    public boolean accepts(Class<?> type)
    {
        return ImmutableList.class.equals(type);
    }

    public ImmutableList create(List<?> items)
    {
        return ImmutableList.copyOf(items);
    }
}
