package org.skife.jdbi.v2;

import java.util.List;

public interface ResultBearing<ResultType> extends Iterable<ResultType>
{
    public <ContainerType> ContainerType list(Class<ContainerType> containerType);
    public List<ResultType> list(final int maxRows);
    public List<ResultType> list();
    public ResultIterator<ResultType> iterator();
    public ResultType first();
    public <T> T first(Class<T> containerType);
}
