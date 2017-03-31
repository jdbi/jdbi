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

package org.skife.jdbi.v2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * A result set mapper which maps the fields in a statement into a
 * <a href="http://en.wikipedia.org/wiki/Plain_Old_Java_Object">POJO</a>
 * (Plain Old Java Object).
 * A POJO is an ordinary Java object which doesn't follow any model, convention
 * or framework.
 * In fact fields are direclty set, even if they are declared private.
 * In order to instantiate the object via reflection, the only requirement is
 * that the POJO has an empty constructor. Also a private empty constructor is
 * OK.
 * Fields are mapped between the table and the POJO case insensitively, removing
 * underscores.
 *
 * <p><strong>Usage recommendation:</strong>
 * The first time the ResultSet is read, the ResultSetMetadata is used to define
 * the mapping between the table columns and the POJO, and it gets cached into
 * the ResultSetMapper instance, once and forever.
 * So you must not use the same mapper for queries returning different sets of
 * columns.<br/>
 * Instead you can use different instances specific for different queries. <br/>
 * This is meant to improve performances, avoiding unneeded repetitive beans
 * introspections and resultset metadata access. </p>
 *
 * Example:
 *
 * <pre>
 * public class User {
 *   public Long userId;
 *   public String firstName;
 *   public String lastName;
 *   public String userName;
 *   public String email;
 *   private String password;
 *
 *   // eventually you may (or may not) have getter and setter
 * }
 * </pre>
 *
 * Notice that the above class has not defined an empty constructor.
 * When no constructor is specified, java defines a default public empty
 * constructor; so it's not necessary to explicitly write one in this case.
 *
 * The above User class be mapped fine to a table defined as:
 *
 * <pre>
 * create table user (
 *   user_id identity not null primary key,
 *   first_name varchar not null,
 *   last_name varchar not null,
 *   user_name varchar not null unique,
 *   email varchar not null unique,
 *   password varchar not null
 * );
 * </pre>
 *
 * Sample usage:
 * <pre>
 * public class UsersDAO {
 *     private DBI dbi = new DBI(...);
 *     private final PojoMapper<User> usersMapper = new PojoMapper<User>(User.class);
 *
 *     public User login(String userName, String password) {
 *         Handle h = dbi.open();
 *         try {
 *             return h.createQuery(
 *                 "select * from user where userName = ? and password = ?")
 *                .bind(0, userName).bind(1, password).map(usersMapper).first();
 *         } finally {
 *             h.close();
 *         }
 *     }
 * }
 */
public class PojoMapper<T> implements ResultSetMapper<T> {
    private final Map<String, Field> fields;
    private final AtomicReference<Map<Integer, String>> columnsRef;
    private final Class<T> c;

    public PojoMapper(Class<T> c) {
        this.c = c;
        fields = introspect(c);
        columnsRef =  new AtomicReference<Map<Integer, String>>();
    }

    @Override
    public T map(int index, ResultSet r, StatementContext ctx)
            throws SQLException {
        try {
            Constructor<T> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            T result = constructor.newInstance();

            Map<Integer, String> columns = threadSafeGetColumns(r.getMetaData());

            for (Map.Entry<Integer, String> col : columns.entrySet()) {
                Integer columnIndex = col.getKey();
                String matchCode = col.getValue();
                Field field = fields.get(matchCode);
                if (field != null)
                    field.set(result, r.getObject(columnIndex));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Map<Integer, String> threadSafeGetColumns(ResultSetMetaData metadata)
            throws SQLException {
        while (true) {
            if (columnsRef.get() != null) {
                return columnsRef.get();
            }
            Map<Integer, String> columnsMap = mapColumns(metadata);
            if (columnsRef.compareAndSet(null, columnsMap)) {
                return columnsRef.get();
            }
        }
    }

    private static Map<String, Field> introspect(Class<?> c) {
        Map<String, Field> map = newMap();
        introspect(c, map);
        return map;
    }

    private static void introspect(Class<?> c, Map<String, Field> map) {
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            String name = matchCode(field.getName());
            if (map.get(name) == null) {
                field.setAccessible(true);
                map.put(name, field);
            }
        }
        Class<?> sup = c.getSuperclass();
        if (sup != null)
            introspect(sup, map);
    }

    private static Map<Integer, String> mapColumns(ResultSetMetaData metaData)
            throws SQLException {
        Map<Integer, String> columnsMap = newMap();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String label = metaData.getColumnLabel(index);
            columnsMap.put(index, matchCode(label));
        }
        return columnsMap;
    }

    /**
     * match code tries to match columns like User_Id into fields like userId
     * This is done removing underscores (commonly used in table fields names)
     * and casting the string to lower case.
     */
    private static String matchCode(String label) {
        return label.replace("_", "").toLowerCase();
    }

    private static <K, V> Map<K, V> newMap() {
        return new LinkedHashMap<K, V>();
    }
}
