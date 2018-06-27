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
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents output from a Call (CallableStatement).
 * @see Call
 */
public class OutParameters {
    private final Map<Object, Object> map = new HashMap<>();

    /**
     * Type-casting convenience method which obtains an object from the map, the
     * object obtained should have been created with {@link CallableStatementMapper}
     *
     * @param name The out parameter name
     * @param type The java type to obtain
     * @param <T> the output parameter type
     * @return the output of name as type T
     */
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
    public <T> T getObject(int pos, Class<T> type) {
        return type.cast(getObject(pos));
    }

    public String getString(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
            }

            return null;
        }

        return obj.toString();
    }

    public String getString(int pos) {
        Object obj = map.get(pos);

        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
            }

            return null;
        }

        return obj.toString();
    }

    public byte[] getBytes(String name) {
        Object obj = map.get(name);
        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
           }

            return null;
        }
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is not byte[] but %s", name, obj.getClass()));
        }
    }

    public byte[] getBytes(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
           }

            return null;
        }

        if (obj instanceof byte[]) {
            return (byte[]) obj;
        } else {
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
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    public Date getDate(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Date(epoch);
    }

    public Timestamp getTimestamp(String name) {
        Long epoch = getEpoch(name);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
    }

    public Timestamp getTimestamp(int pos) {
        Long epoch = getEpoch(pos);

        if (epoch == null) {
            return null;
        }

        return new Timestamp(epoch);
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
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
            }

            return null;
        }

        if (obj instanceof Number) {
            return (Number) obj;
        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is not a number but %s", name, obj.getClass()));
        }
    }

    private Number getNumber(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
            }

            return null;
        }
        if (obj instanceof Number) {
            return (Number) obj;
        } else {
            throw new IllegalArgumentException(String.format("Parameter at %d is not a number but %s", pos, obj.getClass()));
        }
    }

    private Long getEpoch(String name) {
        Object obj = map.get(name);

        if (obj == null) {
            if (!map.containsKey(name)) {
                throw new IllegalArgumentException(String.format("Parameter %s does not exist", name));
            }

            return null;
        }

        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is not Date but %s", name, obj.getClass()));
        }
    }

    private Long getEpoch(int pos) {
        Object obj = map.get(pos);
        if (obj == null) {
            if (!map.containsKey(pos)) {
                throw new IllegalArgumentException(String.format("Parameter at %d does not exist", pos));
            }

            return null;
        }

        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).getTime();
        } else {
            throw new IllegalArgumentException(String.format("Parameter at %d is not Date but %s", pos, obj.getClass()));
        }
    }

    Map<Object, Object> getMap() {
        return map;
    }
}
