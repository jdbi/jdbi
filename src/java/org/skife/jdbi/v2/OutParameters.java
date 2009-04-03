package org.skife.jdbi.v2;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents output from a Call (CallableStatement)
 * @see org.skife.jdbi.v2.Call
 */
public class OutParameters
{
    private final Map map = new HashMap();

    /**
     * Type-casting convenience method which obtains an object from the map, the
     * object obtained should have been created with {@link org.skife.jdbi.v2.CallableStatementMapper}
     *
     * @param name The out parameter name
     * @param type The java type to obtain
     * @return the output of name as type T
     */
    public <T> T getObject(String name, Class<T> type) {
        return type.cast(getObject(name));
    }

    /**
     * Obtains an object from the map, the
     * object obtained should have been created with {@link org.skife.jdbi.v2.CallableStatementMapper}
     *
     * @param name The out parameter name
     * @return the output of name as type T
     */
    public Object getObject(String name) {
        return map.get(name);
    }

    /**
     * Type-casting convenience method which obtains an object from the the results positionally
     * object obtained should have been created with {@link org.skife.jdbi.v2.CallableStatementMapper}
     *
     * @param position The out parameter name
     * @param type The java type to obtain
     * @return the output of name as type T
     */
    public Object getObject(int position) {
        return map.get(position);
    }

    /**
     * Type-casting convenience method which obtains an object from the map positionally
     * object obtained should have been created with {@link org.skife.jdbi.v2.CallableStatementMapper}
     *
     * @param pos The out parameter position
     * @param type The java type to obtain
     * @return the output of name as type T
     */

    public <T> T getObject(int pos, Class<T> type) {
        return type.cast(getObject(pos));
    }

    public String getString(String name) {
        Object obj = map.get(name);
        if (obj != null) {
            return obj.toString();
        }
        throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
    }

    public String getString(int pos) {
        Object obj = map.get(pos);
        if (obj != null) {
            return obj.toString();
        }
        throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
    }

    public byte[] getBytes(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter %s is not byte[] but %s", name, obj.getClass()));
        }
    }

    public byte[] getBytes(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter at %d is not byte[] but %s", pos, obj.getClass()));
        }
    }

    public Integer getInt(String name) {
        return getNumber(name).intValue();
    }

    public Integer getInt(int pos) {
        return getNumber(pos).intValue();
    }

    public Long getLong(String name) {
        return getNumber(name).longValue();
    }

    public Long getLong(int pos) {
        return getNumber(pos).longValue();
    }

    public Short getShort(String name) {
        return getNumber(name).shortValue();
    }

    public Short getShort(int pos) {
        return getNumber(pos).shortValue();
    }

    public Date getDate(String name) {
        return new Date(getEpoch(name));
    }

    public Date getDate(int pos) {
        return new Date(getEpoch(pos));
    }

    public Timestamp getTimestamp(String name) {
        return new Timestamp(getEpoch(name));
    }

    public Timestamp getTimestamp(int pos) {
        return new Timestamp(getEpoch(pos));
    }

    public Double getDouble(String name) {
        return getNumber(name).doubleValue();
    }

    public Double getDouble(int pos) {
        return getNumber(pos).doubleValue();
    }

    public Float getFloat(String name) {
        return getNumber(name).floatValue();
    }

    public Float getFloat(int pos) {
        return getNumber(pos).floatValue();
    }

    private Number getNumber(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
        }
        if (obj instanceof Number) {
            return (Number) obj;
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter %s is not a number but %s", name, obj.getClass()));
        }
    }

    private Number getNumber(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
        }
        if (obj instanceof Number) {
            return (Number) obj;
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter at %d is not a number but %s", pos, obj.getClass()));
        }
    }

    private Long getEpoch(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
        }
        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter %s is not Date but %s", name, obj.getClass()));
        }
    }

    private Long getEpoch(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
        }
        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        }
        else {
            throw new IllegalArgumentException(String.format("Parameter at %d is not Date but %s", pos, obj.getClass()));
        }
    }

    Map getMap() {
        return map;
    }
}
