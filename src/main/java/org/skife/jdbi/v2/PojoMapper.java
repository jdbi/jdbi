package org.skife.jdbi.v2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * So it's recommended not to use the same mapper for queries returning
 * different sets of columns.<br/>
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
 *     private final PojoMapper<User> usersMapper = PojoMapper.get(User.class);
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
 *
 */
public abstract class PojoMapper<T> implements ResultSetMapper<T> {
    private PojoMapper() {}

    public static <T> PojoMapper<T> get(final Class<T> c) {
        final Map<String, Field> fields = introspect(c);
        final Map<Integer, String> columns = newMap();

        return new PojoMapper<T>() {
            @Override
            public T map(int index, ResultSet r, StatementContext ctx)
                    throws SQLException {
                try {
                    Constructor<T> constructor = c.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    T result = constructor.newInstance();
                    if (columns.size() == 0)
                        mapColumns(r.getMetaData(), columns);

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
        };
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

    private static void mapColumns(ResultSetMetaData metaData,
            Map<Integer, String> columnsMap) throws SQLException {
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String label = metaData.getColumnLabel(index);
            columnsMap.put(index, matchCode(label));
        }
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