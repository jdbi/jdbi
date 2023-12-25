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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.jdbi.v3.core.result.ResultBearing;

import static java.lang.String.format;

/**
 * Represents output from a Call (CallableStatement).
 * @see Call
 */
public class OutParameters {
    private final StatementContext ctx;
    private final ResultBearing resultSet;
    private final Map<Object, Object> map = new HashMap<>();

    OutParameters(ResultBearing resultSet, StatementContext ctx) {
        this.resultSet = resultSet;
        this.ctx = ctx;
    }

    /**
     * Returns a ResultBearing backed by the main result set returned by the procedure. This is not
     * supported by all databases.
     *
     * @return a ResultBearing
     */
    public ResultBearing getResultSet() {
        return resultSet;
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
    @Nullable
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
    @Nullable
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
    @Nullable
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
    @Nullable
    public <T> T getObject(int pos, Class<T> type) {
        return type.cast(getObject(pos));
    }

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @Nullable
    public Integer getInt(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.intValue();
    }

    @Nullable
    public Integer getInt(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.intValue();
    }

    @Nullable
    public Long getLong(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.longValue();
    }

    @Nullable
    public Long getLong(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.longValue();
    }

    @Nullable
    public Short getShort(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.shortValue();
    }

    @Nullable
    public Short getShort(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.shortValue();
    }

    @Nullable
    public Date getDate(String name) {
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    @Nullable
    public Date getDate(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    @Nullable
    public Timestamp getTimestamp(String name) {
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
    }

    @Nullable
    public Timestamp getTimestamp(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
    }

    @Nullable
    public Double getDouble(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.doubleValue();
    }

    @Nullable
    public Double getDouble(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.doubleValue();
    }

    @Nullable
    public Float getFloat(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.floatValue();
    }

    @Nullable
    public Float getFloat(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.floatValue();
    }

    @Nonnull
    public ResultBearing getRowSet(String name) {
        return ResultBearing.of(() -> {
            ResultSet resultSet = getObject(name, ResultSet.class);
            ctx.addCleanable(resultSet::close);
            return resultSet;
        }, ctx);
    }

    @Nonnull
    public ResultBearing getRowSet(int pos) {
        return ResultBearing.of(() -> {
            ResultSet resultSet = getObject(pos, ResultSet.class);
            ctx.addCleanable(resultSet::close);
            return resultSet;
        }, ctx);
    }

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @Nullable
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

    @Nonnull
    Map<Object, Object> getMap() {
        return map;
    }
}
