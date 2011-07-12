package org.skife.jdbi.v2;

import java.util.List;

public interface ResultBearing<ResultType> extends Iterable<ResultType>
{
    public List<ResultType> list();
    public ResultIterator<ResultType> iterator();
    public ResultType first();
}
