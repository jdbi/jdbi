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
package org.jdbi.sqlobject.config.internal;

import java.lang.annotation.Annotation;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.enums.EnumStrategy;
import org.jdbi.core.enums.Enums;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.sqlobject.config.UseEnumStrategy;

public class UseEnumStrategyImpl extends SimpleExtensionConfigurer {

    private final EnumStrategy enumStrategy;

    public UseEnumStrategyImpl(Annotation annotation) {
        UseEnumStrategy useEnumStrategy = (UseEnumStrategy) annotation;
        this.enumStrategy = useEnumStrategy.value();
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(Enums.class).setEnumStrategy(enumStrategy);
    }
}
