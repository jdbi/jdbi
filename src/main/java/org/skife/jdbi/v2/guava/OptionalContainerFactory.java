package org.skife.jdbi.v2.guava;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.util.List;

public class OptionalContainerFactory implements ContainerFactory<Optional<?>>
{
    public boolean accepts(Class<?> type)
    {
        return Optional.class.equals(type);
    }

    public Optional<?> create(List<?> items)
    {
        if (items.isEmpty()) {
            return Optional.absent();
        }
        else {
            return Optional.fromNullable(items.get(0));
        }
    }
}
