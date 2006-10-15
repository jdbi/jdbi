package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.DBI;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Proxy;

/**
 *
 */
public class QueryObjectFactory
{
    @SuppressWarnings("unchecked")
    public static <T extends DataAccessor> T createQueryObject(Class<T> queryObjectType, Connection connection) {
        final Class<?>[] interfaces = {queryObjectType};
        return (T) Proxy.newProxyInstance(queryObjectType.getClassLoader(),
                                          interfaces,
                                          new DataAccessHandler(DBI.open(connection)));
    }

    public static <T extends DataAccessor> T createQueryObject(Class<T> queryObjectType, DataSource dataSource)
            throws SQLException
    {
        return createQueryObject(queryObjectType, dataSource.getConnection());
    }
}
