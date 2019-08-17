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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

import static java.util.stream.Collectors.toSet;

/**
 * Utility class for type qualifiers supported by Jdbi core.
 */
@Beta
public class Qualifiers implements JdbiConfig<Qualifiers> {
    private static final JdbiCache<AnnotatedElement[], Set<Annotation>> QUALIFIER_CACHE = JdbiCaches.declare(
        elements -> elements.length == 1 ? elements[0] : new HashSet<>(Arrays.asList(elements)),
        (registry, elements) -> registry.get(Qualifiers.class).resolveQualifiers(elements));
    private final Set<Function<AnnotatedElement, Set<Annotation>>> resolvers = new CopyOnWriteArraySet<>();
    private ConfigRegistry registry;

    public Qualifiers() {
        resolvers.add(Qualifiers::getQualifierAnnotations);
    }

    private Qualifiers(Qualifiers other) {
        this.resolvers.addAll(other.resolvers);
        this.registry = null;
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Adds a qualifier resolver, which inspects annotated elements (e.g. methods or parameters)
     * and returns a set of qualifiers that are found to belong on it. An example resolver is {@link #getQualifierAnnotations},
     * which searches for annotations themselves annotated with {@link Qualifier}.
     *
     * Resolvers should return static results that don't vary over time; therefore their results are cached.
     * Adding a resolver will clear the local qualifier resolution cache, but is currently not guaranteed to have any effect on {@code SqlObjects}.
     * Add any resolvers you may need <em>before</em> using any qualifiers-related features!
     *
     * @param resolver the resolver to be included in qualifier resolution
     * @return this
     */
    public Qualifiers addResolver(Function<AnnotatedElement, Set<Annotation>> resolver) {
        resolvers.add(resolver);
        if (registry != null) {
            QUALIFIER_CACHE.clear(registry);
        }
        return this;
    }

    /**
     * Returns the set of qualifying annotations on the given elements.
     *
     * @param elements the annotated elements. Null elements are ignored.
     * @return the set of qualifying annotations on the given elements.
     */
    public Set<Annotation> findFor(AnnotatedElement... elements) {
        return registry == null
            ? resolveQualifiers(elements)
            : QUALIFIER_CACHE.get(elements, registry);
    }

    private Set<Annotation> resolveQualifiers(AnnotatedElement... elements) {
        Set<AnnotatedElement> nonNullElements = Arrays.stream(elements)
            .filter(Objects::nonNull)
            .collect(toSet());

        return resolvers.stream()
            .flatMap(resolver -> nonNullElements.stream().map(resolver))
            .flatMap(Collection::stream)
            .collect(toSet());
    }

    @Override
    public Qualifiers createCopy() {
        return new Qualifiers(this);
    }

    private static Set<Annotation> getQualifierAnnotations(AnnotatedElement element) {
        return Arrays.stream(element.getAnnotations())
            .filter(anno -> anno.annotationType().isAnnotationPresent(Qualifier.class))
            .collect(toSet());
    }
}
