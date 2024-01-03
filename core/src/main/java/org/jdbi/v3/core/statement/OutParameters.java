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
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.internal.ResultSetSupplier;

import static java.lang.String.format;

/**
 * Holds all output parameters from a stored procedure call using {@link Call}.
 *
 * @see Call
 * @see java.sql.CallableStatement
 */
public class OutParameters {
    private final StatementContext ctx;
    private final ResultBearing resultSet;
    private final ParameterValueMap map = new ParameterValueMap();

    OutParameters(ResultBearing resultSet, StatementContext ctx) {
        this.resultSet = resultSet;
        this.ctx = ctx;
    }

    /**
     * Returns a {@link ResultBearing} backed by the main result set returned by the procedure. This is not supported by all databases.
     *
     * @return A {@link ResultBearing}. This is never null but may be empty.
     * @since 3.43.0
     */
    public ResultBearing getResultSet() {
        return resultSet;
    }

    /**
     * Retrieves an out parameter and casts it to a specific type.
     *
     * @param name The out parameter name
     * @param type The java type to obtain
     * @param <T>  the output parameter type
     * @return the output of name as type T
     * @throws ClassCastException If the parameter can not be cast to type T
     */
    @Nullable
    public <T> T getObject(String name, Class<T> type) {
        return type.cast(getObject(name));
    }

    /**
     * Retrieves an out parameter and casts it to a specific type.
     *
     * @param pos The out parameter index
     * @param type The java type to obtain
     * @param <T>  the output parameter type
     * @return the output of name as type T
     * @throws ClassCastException If the parameter can not be cast to type T
     */
    @Nullable
    public <T> T getObject(int pos, Class<T> type) {
        return type.cast(getObject(pos));
    }

    /**
     * Retrieves an out parameter as an object.
     *
     * @param name The out parameter name
     * @return The value as an Object. Can be null.
     */
    @Nullable
    public Object getObject(String name) {
        return map.getValue(name);
    }

    /**
     * Retrieves an out parameter as an object.
     *
     * @param pos The out parameter index
     * @return The value as an Object. Can be null.
     */
    @Nullable
    public Object getObject(int pos) {
        return map.getValue(pos);
    }

    /**
     * Retrieves an out parameter as a string.
     *
     * @param name The out parameter name
     * @return The value as a String. Can be null.
     */
    @Nullable
    public String getString(String name) {
        Object obj = getObject(name);
        return obj == null ? null : obj.toString();
    }

    /**
     * Retrieves an out parameter as a string.
     *
     * @param pos The out parameter index
     * @return The value as a String. Can be null.
     */
    @Nullable
    public String getString(int pos) {
        Object obj = map.getValue(pos);
        return obj == null ? null : obj.toString();
    }

    /**
     * Retrieves an out parameter as a byte array.
     *
     * @param name The out parameter name
     * @return The value as a byte array. Can be null.
     */
    @Nullable
    public byte[] getBytes(String name) {
        try {
            return getObject(name, byte[].class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter '%s' is not a byte[]", name), e);
        }
    }

    /**
     * Retrieves an out parameter as a byte array.
     *
     * @param pos The out parameter index
     * @return The value as a byte array. Can be null.
     */
    @Nullable
    public byte[] getBytes(int pos) {
        try {
            return getObject(pos, byte[].class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter #%d is not a byte[]", pos), e);
        }
    }

    /**
     * Retrieves an out parameter as a Integer object.
     *
     * @param name The out parameter name
     * @return The value as an Integer object. Can be null.
     */
    @Nullable
    public Integer getInt(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.intValue();
    }

    /**
     * Retrieves an out parameter as a Integer object.
     *
     * @param pos The out parameter index
     * @return The value as an Integer object. Can be null.
     */
    @Nullable
    public Integer getInt(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.intValue();
    }

    /**
     * Retrieves an out parameter as a int value.
     *
     * @param name The out parameter name
     * @return The value as an int. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public int getIntValue(String name) {
        final Number n = getNumber(name);
        return n == null ? 0 : n.intValue();
    }

    /**
     * Retrieves an out parameter as a int value.
     *
     * @param pos The out parameter index
     * @return The value as an int. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public int getIntValue(int pos) {
        final Number n = getNumber(pos);
        return n == null ? 0 : n.intValue();
    }

    /**
     * Retrieves an out parameter as a Long object.
     *
     * @param name The out parameter name
     * @return The value as an Long object. Can be null.
     */
    @Nullable
    public Long getLong(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.longValue();
    }

    /**
     * Retrieves an out parameter as a Long object.
     *
     * @param pos The out parameter index
     * @return The value as an Long object. Can be null.
     */
    @Nullable
    public Long getLong(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.longValue();
    }

    /**
     * Retrieves an out parameter as a long value.
     *
     * @param name The out parameter name
     * @return The value as an long. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public long getLongValue(String name) {
        final Number n = getNumber(name);
        return n == null ? 0L : n.longValue();
    }

    /**
     * Retrieves an out parameter as a long value.
     *
     * @param pos The out parameter index
     * @return The value as an long. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public long getLongValue(int pos) {
        final Number n = getNumber(pos);
        return n == null ? 0L : n.longValue();
    }

    /**
     * Retrieves an out parameter as a Short object.
     *
     * @param name The out parameter name
     * @return The value as an Short object. Can be null.
     */
    @Nullable
    public Short getShort(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.shortValue();
    }

    /**
     * Retrieves an out parameter as a Short object.
     *
     * @param pos The out parameter index
     * @return The value as an Short object. Can be null.
     */
    @Nullable
    public Short getShort(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.shortValue();
    }

    /**
     * Retrieves an out parameter as a short value.
     *
     * @param name The out parameter name
     * @return The value as an short. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public short getShortValue(String name) {
        final Number n = getNumber(name);
        return n == null ? 0 : n.shortValue();
    }

    /**
     * Retrieves an out parameter as a short value.
     *
     * @param pos The out parameter index
     * @return The value as an short. Returns 0 if the value is absent.
     * @since 3.43.0
     */
    public short getShortValue(int pos) {
        final Number n = getNumber(pos);
        return n == null ? 0 : n.shortValue();
    }

    /**
     * Retrieves an out parameter as a {@link Date} object.
     *
     * @param name The out parameter name
     * @return The value as a {@link Date} object. Can be null.
     */
    @Nullable
    public Date getDate(String name) {
        try {
            return getObject(name, Date.class);
        } catch (ClassCastException e) {
            Long epoch = getEpoch(name);
            return epoch == null ? null : new Date(epoch);
        }
    }

    /**
     * Retrieves an out parameter as a {@link Date} object.
     *
     * @param pos The out parameter index
     * @return The value as a {@link Date} object. Can be null.
     */
    @Nullable
    public Date getDate(int pos) {
        try {
            return getObject(pos, Date.class);
        } catch (ClassCastException e) {
            Long epoch = getEpoch(pos);
            return epoch == null ? null : new Date(epoch);
        }
    }

    /**
     * Retrieves an out parameter as a {@link Timestamp} object.
     *
     * @param name The out parameter name
     * @return The value as a {@link Timestamp} object. Can be null.
     */
    @Nullable
    public Timestamp getTimestamp(String name) {
        try {
            return getObject(name, Timestamp.class);
        } catch (ClassCastException e) {
            Long epoch = getEpoch(name);
            return epoch == null ? null : new Timestamp(epoch);
        }
    }

    /**
     * Retrieves an out parameter as a {@link Timestamp} object.
     *
     * @param pos The out parameter index
     * @return The value as a {@link Timestamp} object. Can be null.
     */
    @Nullable
    public Timestamp getTimestamp(int pos) {
        try {
            return getObject(pos, Timestamp.class);
        } catch (ClassCastException e) {
            Long epoch = getEpoch(pos);
            return epoch == null ? null : new Timestamp(epoch);
        }
    }

    /**
     * Retrieves an out parameter as a Double object.
     *
     * @param name The out parameter name
     * @return The value as an Double object. Can be null.
     */
    @Nullable
    public Double getDouble(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.doubleValue();
    }

    /**
     * Retrieves an out parameter as a Double object.
     *
     * @param pos The out parameter index
     * @return The value as an Double object. Can be null.
     */
    @Nullable
    public Double getDouble(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.doubleValue();
    }

    /**
     * Retrieves an out parameter as a double value.
     *
     * @param name The out parameter name
     * @return The value as an double. Returns 0.0d if the value is absent.
     * @since 3.43.0
     */
    public double getDoubleValue(String name) {
        final Number n = getNumber(name);
        return n == null ? 0.0d : n.doubleValue();
    }

    /**
     * Retrieves an out parameter as a double value.
     *
     * @param pos The out parameter index
     * @return The value as an double. Returns 0.0d if the value is absent.
     * @since 3.43.0
     */
    public double getDoubleValue(int pos) {
        final Number n = getNumber(pos);
        return n == null ? 0.0d : n.doubleValue();
    }

    /**
     * Retrieves an out parameter as a Float object.
     *
     * @param name The out parameter name
     * @return The value as an Float object. Can be null.
     */
    @Nullable
    public Float getFloat(String name) {
        final Number n = getNumber(name);
        return n == null ? null : n.floatValue();
    }

    /**
     * Retrieves an out parameter as a Float object.
     *
     * @param pos The out parameter index
     * @return The value as an Float object. Can be null.
     */
    @Nullable
    public Float getFloat(int pos) {
        final Number n = getNumber(pos);
        return n == null ? null : n.floatValue();
    }

    /**
     * Retrieves an out parameter as a float value.
     *
     * @param name The out parameter name
     * @return The value as an float. Returns 0.0f if the value is absent.
     * @since 3.43.0
     */
    public float getFloatValue(String name) {
        final Number n = getNumber(name);
        return n == null ? 0.0f : n.floatValue();
    }

    /**
     * Retrieves an out parameter as a float value.
     *
     * @param pos The out parameter index
     * @return The value as an float. Returns 0.0f if the value is absent.
     * @since 3.43.0
     */
    public float getFloatValue(int pos) {
        final Number n = getNumber(pos);
        return n == null ? 0.0f : n.floatValue();
    }

    /**
     * Retrieves a {@link ResultBearing} for an out parameter. This usually requires that the out parameter is a cursor type.
     * @param name The out parameter name
     * @return A {@link ResultBearing} object representing the out parameter and backed by the cursor.
     * This is not supported by all database drivers. Never null but may be empty.
     */
    @Nonnull
    public ResultBearing getRowSet(String name) {
        return ResultBearing.of(ResultSetSupplier.notClosingContext(() -> getObject(name, ResultSet.class)), ctx);
    }

    /**
     * Retrieves a {@link ResultBearing} for an out parameter. This usually requires that the out parameter is a cursor type.
     * @param pos The out parameter index
     * @return A {@link ResultBearing} object representing the out parameter and backed by the cursor.
     * This is not supported by all database drivers. Never null but may be empty.
     */
    @Nonnull
    public ResultBearing getRowSet(int pos) {
        return ResultBearing.of(ResultSetSupplier.notClosingContext(() -> getObject(pos, ResultSet.class)), ctx);
    }

    @Nullable
    private Number getNumber(String name) {
        try {
            return getObject(name, Number.class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter '%s' is not a number", name), e);
        }
    }

    @Nullable
    private Number getNumber(int pos) {
        try {
            return getObject(pos, Number.class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter #%d is not a number", pos), e);
        }
    }

    @Nullable
    @SuppressWarnings("JavaUtilDate")
    private Long getEpoch(String name) {
        try {
            var date = getObject(name, java.util.Date.class);
            return date == null ? null : date.getTime();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter '%s' is not a java.util.Date", name), e);
        }
    }

    @Nullable
    @SuppressWarnings("JavaUtilDate")
    private Long getEpoch(int pos) {
        try {
            var date = getObject(pos, java.util.Date.class);
            return date == null ? null : date.getTime();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(format("Parameter #%d is not a java.util.Date", pos), e);
        }
    }

    void putValueSupplier(int index, String key, Supplier<Object> value) {
        map.putValueSupplier(index, key, value);
    }

    private static final class ParameterValueMap {
        private final Map<Object, Supplier<Object>> map = new HashMap<>();

        void putValueSupplier(int index, String key, Supplier<Object> value) {
            map.put(index, value);
            if (key != null) {
                map.put(key, value);
            }
        }

        Object getValue(int index) {
            Supplier<Object> supplier = map.get(index);
            if (supplier != null) {
                return supplier.get();
            }

            throw new IllegalArgumentException(format("Parameter #%d does not exist", index));
        }

        Object getValue(String name) {
            Supplier<Object> supplier = map.get(name);
            if (supplier != null) {
                return supplier.get();
            }

            throw new IllegalArgumentException(format("Parameter '%s' does not exist", name));
        }
    }
}
