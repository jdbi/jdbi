package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdbi.v3.SQLStatement;

class BindMapFactory implements BinderFactory
{
    @Override
    public Binder build(Annotation annotation)
    {
        return new Binder<BindMap, Object>()
        {
            @Override
            public void bind(SQLStatement q, BindMap bind, Object arg)
            {
                final String prefix;
                if (BindBean.BARE_BINDING.equals(bind.prefix())) {
                    prefix = "";
                }
                else {
                    prefix = bind.prefix() + ".";
                }

                final Set<String> allowedKeys = new HashSet<String>(Arrays.asList(bind.value()));
                final Map<String, Object> map = (Map<String, Object>) arg;

                try {
                    for (Entry e : map.entrySet()) {
                        final Object keyObj = e.getKey();
                        final String key;
                        if (bind.implicitKeyStringConversion() || (keyObj instanceof String)) {
                            key = keyObj.toString();
                        } else {
                            throw new IllegalArgumentException("Key " + keyObj + " (of " + keyObj.getClass() + ") must be a String");
                        }

                        if (allowedKeys.isEmpty() || allowedKeys.remove(key)) {
                            q.bind(prefix + key, e.getValue());
                        }
                    }

                    // Any leftover keys were specified but not found in the map, so bind as null
                    for (String key : allowedKeys) {
                        final Object val = map.get(key);
                        if (val != null) {
                            throw new IllegalStateException("Internal error: map iteration missed key " + key);
                        }
                        q.bind(prefix + key, val);
                    }
                }
                catch (Exception e) {
                    throw new IllegalStateException("Unable to bind map properties", e);
                }
            }
        };
    }
}
