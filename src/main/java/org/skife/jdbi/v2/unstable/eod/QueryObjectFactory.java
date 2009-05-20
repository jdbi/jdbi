/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.DBI;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

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
