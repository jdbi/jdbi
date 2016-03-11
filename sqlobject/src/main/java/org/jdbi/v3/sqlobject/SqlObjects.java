/*
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
package org.jdbi.v3.sqlobject;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.sqlobject.mixins.CloseMe;

/**
 * Factory class for sql objects.
 */
public class SqlObjects {
    /**
     * Create a sql object of the specified type bound to this handle. Any state changes to the handle, or the sql
     * object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param handle the Handle instance to attach ths sql object to
     * @param sqlObjectType the type of sql object to create
     * @return the new sql object bound to this handle
     */
    public static <T> T attach(Handle handle, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new ConstantHandleDing(handle));
    }

    /**
     * Open a handle and attach a new sql object of the specified type to that handle. Be sure to close the
     * sql object (via a close() method, or calling {@link SqlObjects#close(Object)}
     *
     * @param dbi             the dbi to be used for opening the underlying handle
     * @param sqlObjectType   an interface with annotations declaring desired behavior
     *
     * @return a new sql object of the specified type, with a dedicated handle
     */
    public static <T> T open(DBI dbi, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new ConstantHandleDing(dbi.open()));
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
    public static <T> T onDemand(DBI dbi, Class<T> sqlObjectType)
    {
        return SqlObject.buildSqlObject(sqlObjectType, new OnDemandHandleDing(dbi));
    }

    public static <T, R> R with(DBI dbi, Class<T> sqlObjectType, Function<T, R> handler)
    {
        T sqlObject = open(dbi, sqlObjectType);
        try {
            return handler.apply(sqlObject);
        }
        finally {
            close(sqlObject);
        }
    }

    public static <T> void use(DBI dbi, Class<T> sqlObjectType, Consumer<T> handler)
    {
        with(dbi, sqlObjectType, sqlObject -> {
            handler.accept(sqlObject);
            return null;
        });
    }
    /**
     * Close a sql object which lacks a close() method.
     * @param sqlObject the sql object to close
     */
    public static void close(Object sqlObject)
    {
        if (!(sqlObject instanceof CloseMe)) {
            throw new IllegalArgumentException(sqlObject + " is not a sql object");
        }
        ((CloseMe) sqlObject).close();
    }
}
