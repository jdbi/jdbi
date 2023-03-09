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
package org.jdbi.v3.core.extension;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;

import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.VIRTUAL_FACTORY;

public class ExtensionFrameworkTestFactory implements ExtensionFactory {

    @Override
    public boolean accepts(Class<?> extensionType) {
        return Stream.of(extensionType.getMethods())
                .flatMap(m -> Stream.of(m.getAnnotations()))
                .anyMatch(ExtensionFrameworkTestFactory::matchSqlAnnotations);

    }

    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<FactoryFlag> getFactoryFlags() {
        return EnumSet.of(VIRTUAL_FACTORY);
    }

    private static boolean matchSqlAnnotations(Annotation a) {
        UseExtensionHandler extensionHandlerAnnotation = a.annotationType().getAnnotation(UseExtensionHandler.class);
        return extensionHandlerAnnotation != null && "test".equals(extensionHandlerAnnotation.id());
    }
}
