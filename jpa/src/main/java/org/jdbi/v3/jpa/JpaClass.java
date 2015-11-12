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
package org.jdbi.v3.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableList;

class JpaClass<C> {

    private static final Map<Class<?>, JpaClass<?>> cache = new ConcurrentHashMap<>();

    public static <C> JpaClass<C> get(Class<C> clazz) {
        return (JpaClass<C>) cache.computeIfAbsent(clazz, JpaClass::new);
    }

    private final List<JpaMember> members;

    private JpaClass(Class<C> clazz) {
        this.members = unmodifiableList(new ArrayList<>(inspectClass(clazz)));

        logger.debug("init {}: {} members.", clazz, members.size());
    }

    private static Collection<JpaMember> inspectClass(Class<?> clazz) {
        Map<String, JpaMember> members = new HashMap<>();
        List<String> transients = new ArrayList<>();

        inspectFields(clazz, members, transients);
        inspectAnnotatedProperties(clazz, members, transients);
        inspectSuperclasses(clazz, members, transients);
        inspectNonAnnotatedProperties(clazz, members, transients);
        transients.forEach(members::remove);

        return members.values();
    }

    private static void inspectSuperclasses(Class<?> clazz,
                                            Map<String, JpaMember> members,
                                            List<String> transients) {
        while ((clazz = clazz.getSuperclass()) != null) {
            if (clazz.isAnnotationPresent(MappedSuperclass.class)) {
                inspectFields(clazz, members, transients);
            }
        }
    }

    private static void inspectFields(Class<?> clazz,
                                      Map<String, JpaMember> members,
                                      List<String> transients) {
        for (Field member : clazz.getDeclaredFields()) {
            if (member.getAnnotation(Transient.class) != null) {
                transients.add(member.getName());
                continue;
            }

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
                                                   Map<String, JpaMember> members,
                                                   List<String> transients) {
        inspectProperties(clazz, members, transients, true);
    }

    private static void inspectNonAnnotatedProperties(Class<?> clazz,
                                                   Map<String, JpaMember> members,
                                                   List<String> transients) {
        inspectProperties(clazz, members, transients, false);
    }

    private static void inspectProperties(Class<?> clazz,
                                          Map<String, JpaMember> members,
                                          List<String> transients,
                                          boolean hasColumnAnnotation) {
        try {
            Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                    .filter(property -> !members.containsKey(property.getName()))
                    .filter(property -> !transients.contains(property.getName()))
                    .filter(property -> !(property instanceof IndexedPropertyDescriptor))
                    .filter(property -> !"class".equals(property.getName()))
                    .forEach(property -> {
                        Method getter = property.getReadMethod();
                        Method setter = property.getWriteMethod();

                        boolean isTransient = Arrays
                                .stream(new Method[]{getter, setter})
                                .filter(Objects::nonNull)
                                .map(method -> method.getAnnotation(Transient.class))
                                .anyMatch(Objects::nonNull);

                        if (isTransient) {
                            transients.add(property.getName());
                            return;
                        }

                        Column column = Arrays
                                .stream(new Method[]{getter, setter})
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
            logger.warn("Unable to introspect " + clazz, e);
            return;
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

    private static final Logger logger = LoggerFactory.getLogger(JpaClass.class);
}
