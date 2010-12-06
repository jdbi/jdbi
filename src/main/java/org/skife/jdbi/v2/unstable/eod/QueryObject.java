package org.skife.jdbi.v2.unstable.eod;

import java.util.List;

public interface QueryObject<T>
{
    void setTimeout(int seconds);

    void setFetchSize(int fetchSize);

    List<T> list();
}
