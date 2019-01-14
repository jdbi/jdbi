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
package org.jdbi.v3.jpa.internal;

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableList;

public class JpaClass<C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaClass.class);
    private static final Map<Class<?>, JpaClass<?>> CACHE = synchronizedMap(new WeakHashMap<>());

    @SuppressWarnings("unchecked")
    public static <C> JpaClass<C> get(Class<C> clazz) {
        return (JpaClass<C>) CACHE.computeIfAbsent(clazz, JpaClass::new);
    }

    private final List<JpaMember> members;

    private JpaClass(Class<C> clazz) {
        this.members = unmodifiableList(new ArrayList<>(inspectClass(clazz)));

        LOGGER.debug("init {}: {} members.", clazz, members.size());
    }

    private static Collection<JpaMember> inspectClass(Class<?> clazz) {
        Map<String, JpaMember> members = new HashMap<>();

        inspectFields(clazz, members);
        inspectAnnotatedProperties(clazz, members);
        inspectSuperclasses(clazz, members);
        inspectNonAnnotatedProperties(clazz, members);

        return members.values();
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private static void inspectSuperclasses(Class<?> clazz,
                                            Map<String, JpaMember> members) {
        Class<?> c = clazz;
        while ((c = c.getSuperclass()) != null) {
            if (c.isAnnotationPresent(MappedSuperclass.class)) {
                inspectFields(c, members);
            }
        }
    }

    private static void inspectFields(Class<?> clazz,
                                      Map<String, JpaMember> members) {
        for (Field member : clazz.getDeclaredFields()) {
            if (members.containsKey(member.getName())) {
                continue;
            }

            Column column = member.getAnnotation(Column.class);
            if (column != null) {
                members.put(member.getName(), new JpaMember(clazz, column, member));
            }
        }
    }

    private static void inspectAnnotatedProperties(Class<?> clazz,
                                                   Map<String, JpaMember> members) {
        inspectProperties(clazz, members, true);
    }

    private static void inspectNonAnnotatedProperties(Class<?> clazz,
                                                   Map<String, JpaMember> members) {
        inspectProperties(clazz, members, false);
    }

    private static void inspectProperties(Class<?> clazz,
                                          Map<String, JpaMember> members,
                                          boolean hasColumnAnnotation) {
        try {
            Stream.of(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                    .filter(property -> !members.containsKey(property.getName()))
                    .filter(property -> !(property instanceof IndexedPropertyDescriptor))
                    .filter(property -> !"class".equals(property.getName()))
                    .forEach(property -> {
                        Method getter = property.getReadMethod();
                        Method setter = property.getWriteMethod();

                        Column column = Stream.of(getter, setter)
                                .filter(Objects::nonNull)
                                .map(method -> method.getAnnotation(Column.class))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                        if ((column != null) == hasColumnAnnotation) {
                            members.put(property.getName(), new JpaMember(clazz, column, property));
                        }
                    });
        } catch (IntrospectionException e) {
            LOGGER.warn("Unable to introspect " + clazz, e);
        }
    }

    public JpaMember lookupMember(String columnLabel) {
        String column = columnLabel.toLowerCase(Locale.ROOT);
        return members.stream()
                .filter(member -> column.equals(member.getColumnName().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    public List<JpaMember> members() {
        return members;
    }
}
