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
package org.jdbi.v3.core.qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.meta.Beta;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 * Utility class for type qualifiers supported by Jdbi core.
 */
@Beta
public class Qualifiers implements JdbiConfig<Qualifiers> {
    private static final JdbiCache<AnnotatedElement[], Set<Annotation>> QUALIFIER_CACHE = JdbiCaches.declare(
            elements -> elements.length == 1 ? elements[0] : new HashSet<>(Arrays.asList(elements)),
            Qualifiers::getQualifiers);
    private ConfigRegistry registry;

    public Qualifiers() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the set of qualifying annotations on the given elements.
     * @param elements the annotated elements. Null elements are ignored.
     * @return the set of qualifying annotations on the given elements.
     */
    public Set<Annotation> findFor(AnnotatedElement... elements) {
        if (registry == null) {
            return getQualifiers(elements);
        }
        return QUALIFIER_CACHE.get(elements, registry);
    }

    private static Set<Annotation> getQualifiers(AnnotatedElement... elements) {
        Stream<Annotation> directQualifiers = Arrays.stream(elements)
            .filter(Objects::nonNull)
            .map(AnnotatedElement::getAnnotations)
            .flatMap(Arrays::stream)
            .filter(anno -> anno.annotationType().isAnnotationPresent(Qualifier.class));

        Stream<Annotation> indirectQualifiers = Arrays.stream(elements)
            .filter(Objects::nonNull)
            .map(AnnotatedElement::getAnnotations)
            .flatMap(Arrays::stream)
            .filter(anno -> anno instanceof Qualified)
            .map(Qualified.class::cast)
            .map(Qualified::value)
            .flatMap(Arrays::stream)
            .map(AnnotationFactory::create);

        return Stream.concat(directQualifiers, indirectQualifiers)
            .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
    }

    @Override
    public Qualifiers createCopy() {
        return this;
    }
}
