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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jdbi.v3.core.internal.AnnotationFactory;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;

class QualifierSet {
    private final Annotation single;
    private final Set<Annotation> multiple;

    private QualifierSet() {
        single = null;
        multiple = null;
    }

    private QualifierSet(Annotation qualifier) {
        single = qualifier;
        multiple = null;
    }

    private QualifierSet(Set<Annotation> qualifiers) {
        single = null;
        multiple = unmodifiableSet(qualifiers);
    }

    Set<Annotation> getQualifiers() {
        if (single == null && multiple == null) {
            return Collections.emptySet();
        }

        // could omit this if we're confident enough and want to skip everything we can
        if (single != null && multiple != null) {
            throw new IllegalStateException();
        }

        if (single == null) {
            return multiple;
        } else {
            return singleton(single);
        }
    }

    boolean qualifiersEqualTo(Set<Annotation> annotations) {
        if (single == null && multiple == null) {
            return annotations.isEmpty();
        }

        // could omit this if we're confident enough and want to skip everything we can
        if (single != null && multiple != null) {
            throw new IllegalStateException();
        }

        if (single == null) {
            return annotations.equals(multiple);
        } else {
            Iterator<Annotation> iter = annotations.iterator();
            return iter.hasNext()
                && iter.next().equals(single)
                && !iter.hasNext();
        }
    }

    boolean contains(Class<? extends Annotation> qualifier) {
        return multiple != null && multiple.stream().anyMatch(qualifier::isInstance)
            || qualifier.isInstance(single);
    }

    static QualifierSet empty() {
        return new QualifierSet();
    }

    static QualifierSet ofInstances(Annotation... qualifiers) {
        switch (qualifiers.length) {
            case 0:
                return new QualifierSet();
            case 1:
                return new QualifierSet(qualifiers[0]);
            default:
                Set<Annotation> distinct = new HashSet<>(Arrays.asList(qualifiers));
                return distinct.size() == 1 ? new QualifierSet(qualifiers[0]) : new QualifierSet(distinct);
        }
    }

    static QualifierSet ofInstances(Iterable<? extends Annotation> qualifiers) {
        Iterator<? extends Annotation> it = qualifiers.iterator();
        if (it.hasNext()) {
            List<Annotation> list = new ArrayList<>();
            it.forEachRemaining(list::add);
            return ofInstances(list.toArray(new Annotation[0]));
        } else {
            return empty();
        }
    }

    @SafeVarargs
    static QualifierSet ofClasses(Class<? extends Annotation>... qualifiers) {
        switch (qualifiers.length) {
            case 0:
                return new QualifierSet();
            case 1:
                return new QualifierSet(AnnotationFactory.create(qualifiers[0]));
            default:
                Annotation[] distinct = Arrays.stream(qualifiers)
                    .distinct()
                    .map(AnnotationFactory::create)
                    .toArray(Annotation[]::new);
                return distinct.length == 1 ? new QualifierSet(distinct[0]) : new QualifierSet(new HashSet<>(Arrays.asList(distinct)));
        }
    }

    static QualifierSet ofClasses(Iterable<Class<? extends Annotation>> qualifiers) {
        Iterator<Class<? extends Annotation>> it = qualifiers.iterator();
        if (it.hasNext()) {
            List<Class<? extends Annotation>> list = new ArrayList<>();
            it.forEachRemaining(list::add);
            return ofClasses(list.toArray(new Class[0]));
        } else {
            return empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QualifierSet qualifierSet = (QualifierSet) o;
        return Objects.equals(single, qualifierSet.single)
            && Objects.equals(multiple, qualifierSet.multiple);
    }

    @Override
    public int hashCode() {
        return Objects.hash(single, multiple);
    }
}
