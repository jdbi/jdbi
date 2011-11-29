package org.skife.jdbi.v2.tweak;

import java.util.List;

public interface ContainerFactory<T>
{
    public boolean accepts(Class<?> type);

    public T create(List<?> items);
}
