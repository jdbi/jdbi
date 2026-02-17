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
import java.lang.reflect.Type;
import java.util.stream.Collector;

import org.jdbi.core.collector.JdbiCollectors;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.sqlobject.config.RegisterCollector;

public class RegisterCollectorImpl extends SimpleExtensionConfigurer {

    private final Type resultType;
    private final Collector<?, ?, ?> collector;

    public RegisterCollectorImpl(Annotation annotation) {
        final RegisterCollector registerCollector = (RegisterCollector) annotation;
        final Class<? extends Collector<?, ?, ?>> klass = registerCollector.value();

        this.resultType = GenericTypes.findGenericParameter(klass, Collector.class, 2)
                .orElseThrow(() -> new IllegalArgumentException("Tried to pass non-collector object to @RegisterCollector"));
        this.collector = JdbiClassUtils.checkedCreateInstance(klass);
    }

    @Override
    public void configure(final ConfigRegistry config, final Annotation annotation, final Class<?> extensionType) {
        final JdbiCollectors collectors = config.get(JdbiCollectors.class);
        collectors.registerCollector(resultType, collector);
    }
}
