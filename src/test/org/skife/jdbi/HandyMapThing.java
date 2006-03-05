package org.skife.jdbi;

import java.util.HashMap;

/**
 * 
 */
public class HandyMapThing<K> extends HashMap<K, Object>
{
    public HandyMapThing<K> add(K k, Object v)
    {
        this.put(k, v);
        return this;
    }
}
