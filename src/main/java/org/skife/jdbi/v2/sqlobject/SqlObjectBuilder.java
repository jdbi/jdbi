/*
 * Copyright 2004 - 2011 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

/**
 * All methods will be moved onto DBI or Handle when this moves out of unstable.
 */
public class SqlObjectBuilder
{
    public static <T> T attach(Handle handle, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new FixedHandleDing(handle));
    }

    public static <T> T open(DBI dbi, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new FixedHandleDing(dbi.open()));
    }

    /**
     * Creating a sql object via this method will cause it to obtain a database connection when it is needed, and
     * release it as soon as it is able to. This means to two calls, back to back, will cause the sql object to obtain
     * a database connection separately for each one.
     * <p/>
     * In the case of a transaction, via {@link Transactional}, the database connection which started the
     * transaction will be held until the transaction is completed.
     *
     * @param dbi           source of database connections
     * @param sqlObjectType the type of sql object to instantiate
     *
     * @return an on-demand sql object
     */
    public static <T> T onDemand(final DBI dbi, final Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new OnDemandHandleDing(dbi));
    }

    public static void close(Object sqlObject)
    {
        SqlObject.close(sqlObject);
    }
}
