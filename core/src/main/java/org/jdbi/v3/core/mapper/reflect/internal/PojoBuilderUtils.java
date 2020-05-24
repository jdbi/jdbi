package org.jdbi.v3.core.mapper.reflect.internal;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

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

public class PojoBuilderUtils {
    static String propertyName(Method m) {
        final String[] prefixes = new String[] {"get", "is"};
        ColumnName colName = m.getAnnotation(ColumnName.class);
        if (colName != null) {
            return colName.value();
        }
        final String name = m.getName();
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)
                && name.length() > prefix.length()
                && Character.isUpperCase(name.charAt(prefix.length()))) {
                return chopPrefix(name, prefix.length());
            }
        }
        return name;
    }

    private static String chopPrefix(final String name, int off) {
        return name.substring(off, off + 1).toLowerCase() + name.substring(off + 1);
    }

    public static boolean isProperty(Method m) {
        return m.getParameterCount() == 0
            && !m.isSynthetic()
            && !Modifier.isStatic(m.getModifiers())
            && m.getDeclaringClass() != Object.class;
    }

    public static MethodHandle findBuilderSetter(final Class<?> builderClass, String name, Method decl, Type type)
        throws IllegalAccessException, NoSuchMethodException {
        final List<NoSuchMethodException> failures = new ArrayList<>();
        final Set<String> names = new LinkedHashSet<>();
        names.add(decl.getName());
        names.add(name);
        if (name.length() > 1) {
            final String rest = name.substring(0, 1).toUpperCase() + name.substring(1);
            names.add("set" + rest);
            names.add("is" + rest);
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
}
