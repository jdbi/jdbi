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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.UtilityClassException;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class PojoBuilderUtils {
    private static final String[] GETTER_PREFIXES = new String[] {"get", "is"};
    private PojoBuilderUtils() {
        throw new UtilityClassException();
    }

    public static boolean isProperty(Method m) {
        return m.getParameterCount() == 0
            && !m.isSynthetic()
            && !Modifier.isStatic(m.getModifiers())
            && m.getDeclaringClass() != Object.class;
    }

    public static String propertyName(Method m) {
        ColumnName colName = m.getAnnotation(ColumnName.class);
        if (colName != null) {
            return colName.value();
        }
        return defaultSetterName(m.getName());
    }

    public static String defaultSetterName(String name) {
        for (String prefix : GETTER_PREFIXES) {
            if (name.startsWith(prefix)
                && name.length() > prefix.length()
                && Character.isUpperCase(name.charAt(prefix.length()))) {
                return chopPrefix(name, prefix.length());
            }
        }
        return name;
    }

    public static String chopPrefix(final String name, int off) {
        return name.substring(off, off + 1).toLowerCase() + name.substring(off + 1);
    }

    private static Set<String> setterNames(String name) {
        final Set<String> names = new LinkedHashSet<>();
        final String rest = name.substring(0, 1).toUpperCase() + name.substring(1);

        names.add("set" + rest);
        names.add("is" + rest);

        return names;
    }

    public static MethodHandle findBuilderSetter(final Class<?> builderClass, String name, Method decl, Type type)
        throws IllegalAccessException {
        final List<NoSuchMethodException> failures = new ArrayList<>();
        final Set<String> names = new LinkedHashSet<>();
        names.add(PojoBuilderUtils.defaultSetterName(decl.getName()));
        names.add(name);
        if (name.length() > 1) {
            names.addAll(setterNames(name));
        }
        ColumnName columnName = decl.getAnnotation(ColumnName.class);
        if (columnName != null && columnName.value().equals(name)) {
            names.addAll(setterNames(PojoBuilderUtils.defaultSetterName(decl.getName())));
        }
        for (String tryName : names) {
            try {
                return MethodHandles.lookup().unreflect(builderClass.getMethod(tryName, GenericTypes.getErasedType(type)));
            } catch (NoSuchMethodException e) {
                failures.add(e);
            }
        }
        for (Method m : builderClass.getMethods()) {
            if (names.contains(m.getName()) && m.getParameterCount() == 1) {
                return MethodHandles.lookup().unreflect(m);
            }
        }
        final IllegalArgumentException iae = new IllegalArgumentException("Failed to find builder setter for property " + name + " on " + builderClass);
        failures.forEach(iae::addSuppressed);
        return MethodHandles.dropArguments(
            MethodHandles.throwException(Object.class, IllegalArgumentException.class),
            1, Arrays.asList(Object.class, Object.class))
            .bindTo(iae);
    }

    public static MethodHandle alwaysSet() {
        return MethodHandles.dropArguments(MethodHandles.constant(boolean.class, true), 0, Object.class);
    }
}
