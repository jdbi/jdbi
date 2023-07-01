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
package org.jdbi.v3.sqlobject.locator.internal;

import java.lang.annotation.Annotation;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.locator.AnnotationSqlLocator;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

public class UseAnnotationSqlLocatorImpl extends SimpleExtensionConfigurer {

    private final SqlLocator sqlLocator = new AnnotationSqlLocator();

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(SqlObjects.class).setSqlLocator(sqlLocator);
    }
}
