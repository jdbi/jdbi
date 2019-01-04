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
package org.jdbi.v3.core.argument.internal;

import org.jdbi.v3.core.argument.BeanPropertyArguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.mapper.reflect.internal.PojoPropertiesFactories;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * This class only exists to use the protected BeanPropertyArguments constructor.
 * When we can remove that class from public API, this class will easily merge into it.
 */
@SuppressWarnings("deprecation")
public class PojoPropertyArguments extends BeanPropertyArguments {
    private final StatementContext ctx;

    public PojoPropertyArguments(String prefix, Object bean, StatementContext ctx) {
        super(prefix, bean, ctx.getConfig(PojoPropertiesFactories.class).propertiesOf(bean.getClass()));
        this.ctx = ctx;
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object o) {
        return new PojoPropertyArguments(null, o, ctx);
    }
}
