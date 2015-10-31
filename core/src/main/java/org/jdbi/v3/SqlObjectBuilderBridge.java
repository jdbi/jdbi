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
package org.jdbi.v3;

import org.jdbi.v3.exceptions.DBIException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A bridge to the <b>SqlObjectBuilder</b> class in the <b>jdbi3-sqlobject</b> module.
 */
class SqlObjectBuilderBridge {

    private static final String SQL_OBJECT_BUILDER_CLASS = "org.jdbi.v3.sqlobject.SqlObjectBuilder";

    private static final MethodHandle ATTACH;
    private static final MethodHandle OPEN;
    private static final MethodHandle ON_DEMAND;
    private static final MethodHandle CLOSE;

    static {
        Class<?> sqlObjectBuilder = null;
        try {
            sqlObjectBuilder = Class.forName(SQL_OBJECT_BUILDER_CLASS);
        } catch (ClassNotFoundException ignore) {
        }

        if (sqlObjectBuilder != null) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                ATTACH = lookup.findStatic(sqlObjectBuilder, "attach",
                        MethodType.methodType(Object.class, Handle.class, Class.class));
                OPEN = lookup.findStatic(sqlObjectBuilder, "open",
                        MethodType.methodType(Object.class, DBI.class, Class.class));
                ON_DEMAND = lookup.findStatic(sqlObjectBuilder, "onDemand",
                        MethodType.methodType(Object.class, DBI.class, Class.class));
                CLOSE = lookup.findStatic(sqlObjectBuilder, "close",
                        MethodType.methodType(void.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException("Unable to locate methods of SqlObjectBuilder", e);
            }
        } else {
            ATTACH = null;
            OPEN = null;
            ON_DEMAND = null;
            CLOSE = null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T attach(Handle handle, Class<T> sqlObjectType) {
        try {
            check(ATTACH);
            return (T) ATTACH.invoke(handle, sqlObjectType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Error during `SqlObjectBuilder.attach` invocation", t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T open(DBI dbi, Class<T> sqlObjectType) {
        try {
            check(OPEN);
            return (T) OPEN.invoke(dbi, sqlObjectType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Error during `SqlObjectBuilder.open` invocation", t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T onDemand(DBI dbi, Class<T> sqlObjectType) {
        try {
            check(ON_DEMAND);
            return (T) ON_DEMAND.invoke(dbi, sqlObjectType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Error during `SqlObjectBuilder.onDemand` invocation", t);
        }
    }

    public static void close(Object sqlObject) {
        try {
            check(CLOSE);
            CLOSE.invoke(sqlObject);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Error during `SqlObjectBuilder.close` invocation", t);
        }
    }

    private static void check(MethodHandle mh) {
        if (mh == null) {
            throw new IllegalStateException("SQL Object API is not available");
        }
    }
}
