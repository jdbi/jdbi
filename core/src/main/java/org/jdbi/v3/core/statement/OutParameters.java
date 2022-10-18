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
package org.jdbi.v3.core.statement;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jdbi.v3.core.result.ResultBearing;

import static java.lang.String.format;

/**
 * Represents output from a Call (CallableStatement).
 * @see Call
 */
public class OutParameters {
    private final StatementContext ctx;
    private final Map<Object, Object> map = new HashMap<>();

    OutParameters(StatementContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Type-casting convenience method which obtains an object from the map, the
     * object obtained should have been created with {@link CallableStatementMapper}
     *
     * @param name The out parameter name
     * @param type The java type to obtain
     * @param <T> the output parameter type
     * @return the output of name as type T
     */
    @CheckForNull
    public <T> T getObject(String name, Class<T> type) {
        return type.cast(getObject(name));
    }

    /**
     * Obtains an object from the map, the
     * object obtained should have been created with {@link CallableStatementMapper}
     *
     * @param name The out parameter name
     * @return the output of name as type T
     */
    @CheckForNull
    public Object getObject(String name) {
        return map.get(name);
    }

    /**
     * Type-casting convenience method which obtains an object from the results positionally
     * object obtained should have been created with {@link CallableStatementMapper}
     *
     * @param position The out parameter name
     * @return the output of name as type T
     */
    @CheckForNull
    public Object getObject(int position) {
        return map.get(position);
    }

    /**
     * Type-casting convenience method which obtains an object from the map positionally
     * object obtained should have been created with {@link CallableStatementMapper}
     *
     * @param pos The out parameter position
     * @param type The java type to obtain
     * @param <T> the output parameter type
     * @return the output of name as type T
     */
    @CheckForNull
    public <T> T getObject(int pos, Class<T> type) {
        return type.cast(getObject(pos));
    }

    @CheckForNull
    public String getString(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(format("Parameter '%s' does not exist", name));
            }

            return null;
        }

        return obj.toString();
    }

    @CheckForNull
    public String getString(int pos) {
        Object obj = map.get(pos);

        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(format("Parameter at %d does not exist", pos));
            }

            return null;
        }

        return obj.toString();
    }

    @CheckForNull
    public byte[] getBytes(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(format("Parameter '%s' does not exist", name));
            }

            return null;
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        } else {
            throw new IllegalArgumentException(format("Parameter '%s' is a %s, not a byte[]", name, obj.getClass()));
        }
    }

    @CheckForNull
    public byte[] getBytes(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(format("Parameter at %d does not exist", pos));
            }

            return null;
        }

        if (obj instanceof byte[]) {
            return (byte[]) obj;
        } else {
            throw new IllegalArgumentException(format("Parameter at %d is a %s, not a byte[]", pos, obj.getClass()));
        }
    }

    @CheckForNull
    public Integer getInt(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.intValue();
    }

    @CheckForNull
    public Integer getInt(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.intValue();
    }

    @CheckForNull
    public Long getLong(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.longValue();
    }

    @CheckForNull
    public Long getLong(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.longValue();
    }

    @CheckForNull
    public Short getShort(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.shortValue();
    }

    @CheckForNull
    public Short getShort(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.shortValue();
    }

    @CheckForNull
    public Date getDate(String name) {
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    @CheckForNull
    public Date getDate(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    @CheckForNull
    public Timestamp getTimestamp(String name) {
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
    }

    @CheckForNull
    public Timestamp getTimestamp(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
    }

    @CheckForNull
    public Double getDouble(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.doubleValue();
    }

    @CheckForNull
    public Double getDouble(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.doubleValue();
    }

    @CheckForNull
    public Float getFloat(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.floatValue();
    }

    @CheckForNull
    public Float getFloat(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.floatValue();
    }

    @NonNull
    public ResultBearing getRowSet(String name) {
        return ResultBearing.of(() -> {
            ResultSet resultSet = getObject(name, ResultSet.class);
            ctx.addCleanable(resultSet::close);
            return resultSet;
        }, ctx);
    }

    @NonNull
    public ResultBearing getRowSet(int pos) {
        return ResultBearing.of(() -> {
            ResultSet resultSet = getObject(pos, ResultSet.class);
            ctx.addCleanable(resultSet::close);
            return resultSet;
        }, ctx);
    }

    @CheckForNull
    private Number getNumber(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(format("Parameter '%s' does not exist", name));
            }
            return null;
        }

        if (obj instanceof Number) {
            return (Number) obj;
        } else {
            throw new IllegalArgumentException(format("Parameter '%s' is a %s, not a number", name, obj.getClass()));
        }
    }

    @CheckForNull
    private Number getNumber(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(format("Parameter %d does not exist", pos));
            }
            return null;
        }

        if (obj instanceof Number) {
            return (Number) obj;
        } else {
            throw new IllegalArgumentException(format("Parameter %d is a %s, not a number", pos, obj.getClass()));
        }
    }

    @CheckForNull
    @SuppressWarnings("JavaUtilDate")
    private Long getEpoch(String name) {
        Object obj = map.get(name);

        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(format("Parameter '%s' does not exist", name));
            }

            return null;
        }

        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        } else {
            throw new IllegalArgumentException(format("Parameter '%s' is a %s, not a Date", name, obj.getClass()));
        }
    }

    @CheckForNull
    @SuppressWarnings("JavaUtilDate")
    private Long getEpoch(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(format("Parameter at %d does not exist", pos));
            }

            return null;
        }

        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        } else {
            throw new IllegalArgumentException(format("Parameter at %d is a %s, not a Date", pos, obj.getClass()));
        }
    }

    @NonNull
    Map<Object, Object> getMap() {
        return map;
    }
}
