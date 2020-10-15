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
package org.jdbi.v3.core.argument;

import org.jdbi.v3.core.argument.internal.PojoPropertyArguments;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory;

/**
 * Inspect a {@link java.beans} style object and bind parameters
 * based on each of its discovered properties.
 *
 * @deprecated this should never have been public API
 */
@Deprecated
public class BeanPropertyArguments extends PojoPropertyArguments {
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public BeanPropertyArguments(String prefix, Object bean) {
        this(prefix, bean, new ConfigRegistry());
    }

    public BeanPropertyArguments(String prefix, Object bean, ConfigRegistry config) {
        super(prefix, bean, BeanPropertiesFactory.propertiesFor(bean.getClass(), config), config);
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(TypedValue o) {
        return new BeanPropertyArguments(null, o.getValue(), config);
    }
}
