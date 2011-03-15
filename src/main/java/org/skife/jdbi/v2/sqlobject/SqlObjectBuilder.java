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
 * This duplicates the API on {@link DBI} and {@link Handle} for creating sql objects. While it is fine to use these
 * methods to create sql objects, there is no real difference between them and the oones on DBI and Handle.
 */
public class SqlObjectBuilder
{

    /**
     * Create a a sql object of the specified type bound to this handle. Any state changes to the handle, or the
     * sql object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param handle the Handle instance to attach ths sql object to
     * @param sqlObjectType the type of sql object to create
     * @return the new sql object bound to this handle
     */
    public static <T> T attach(Handle handle, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new FixedHandleDing(handle));
    }

    /**
     * Open a handle and attach a new sql object of the specified type to that handle. Be sure to close the
     * sql object (via a close() method, or calling {@link org.skife.jdbi.v2.IDBI#close(Object)}
     *
     * @param dbi             the dbi to be used for opening the underlying handle
     * @param sqlObjectType   an interface with annotations declaring desired behavior
     *
     * @return a new sql object of the specified type, with a dedicated handle
     */
    public static <T> T open(DBI dbi, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new FixedHandleDing(dbi.open()));
    }

    /**
     * Create a new sql object which will obtain and release connections from this dbi instance, as it needs to,
     * and can, respectively. You should not explicitely close this sql object
     *
     * @param dbi the DBI instance to be used for obtaining connections when they are required
     * @param sqlObjectType   an interface with annotations declaring desired behavior
     *
     * @return a new sql object of the specified type, with a dedicated handle
     */
    public static <T> T onDemand(final DBI dbi, final Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new OnDemandHandleDing(dbi));
    }

    /**
     * Used to close a sql object which lacks a close() method.
     * @param sqlObject the sql object to close
     */
    public static void close(Object sqlObject)
    {
        SqlObject.close(sqlObject);
    }
}
