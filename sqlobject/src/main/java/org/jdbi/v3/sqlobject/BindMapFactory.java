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
package org.jdbi.v3.sqlobject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class BindMapFactory implements BinderFactory<BindMap, Map<String, Object>>
{
    @Override
    public Binder<BindMap, Map<String, Object>> build(BindMap annotation)
    {
        return (q, param, bind, arg) -> {
            final String prefix;
            if (BindBean.BARE_BINDING.equals(bind.prefix())) {
                prefix = "";
            }
            else {
                prefix = bind.prefix() + ".";
            }

            final Set<String> allowedKeys = new HashSet<String>(Arrays.asList(bind.value()));
            final Map<String, Object> map = arg;

            for (Entry<String, Object> e : map.entrySet()) {
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
                q.bind(prefix + key, (Object) null);
            }
        };
    }
}
