package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface MapperFactory<T>
{

    public boolean accepts(Class type);

    public ResultSetMapper mapperFor(Class type);
}
