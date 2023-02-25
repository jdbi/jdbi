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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdbi.v3.core.internal.UtilityClassException;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

import static java.lang.invoke.MethodHandles.Lookup.PACKAGE;
import static java.lang.invoke.MethodHandles.Lookup.PRIVATE;
import static java.lang.invoke.MethodHandles.Lookup.PROTECTED;
import static java.lang.invoke.MethodHandles.Lookup.PUBLIC;
import static java.util.Collections.synchronizedMap;

/**
 * this is a required kludge for JDK 8. Remove once we move past JDK8 compatibility.
 */
final class PrivateLookupInKludge {

    private PrivateLookupInKludge() {
        throw new UtilityClassException();
    }

    private static final int ANY_ACCESS = PUBLIC | PRIVATE | PROTECTED | PACKAGE;
    // MethodHandles.privateLookupIn(Class, Lookup) was added in JDK 9.
    // JDK 9 allows us to unreflectSpecial() on an interface default method, where JDK 8 did not.
    private static final Method PRIVATE_LOOKUP_IN = privateLookupIn();
    private static final Map<Class<?>, Lookup> PRIVATE_LOOKUPS = synchronizedMap(new WeakHashMap<>());

    private static Method privateLookupIn() {
        try {
            return MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException ignored) {
            // Method was added in JDK 9. - @TODO remove this hack when we move to JDK11+
            return null;
        }
    }

    static MethodHandles.Lookup lookupFor(Class<?> clazz) {
        if (PRIVATE_LOOKUP_IN != null) {
            try {
                return (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, clazz, MethodHandles.lookup());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnableToCreateExtensionException(e,
                        "Error invoking MethodHandles.privateLookupIn(%s.class, MethodHandles.lookup()) in JDK 9+ runtime", clazz);
            }
        }

        // TERRIBLE, HORRIBLE, NO GOOD, VERY BAD HACK
        // Courtesy of:
        // https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/

        // We can use MethodHandles to look up and invoke the super method, but since this class is not an
        // implementation of method.getDeclaringClass(), MethodHandles.Lookup will throw an exception since
        // this class doesn't have access to the super method, according to Java's access rules. This horrible,
        // awful workaround allows us to directly invoke MethodHandles.Lookup's private constructor, bypassing
        // the usual access checks.

        // This workaround is only used in JDK 8.x runtimes. JDK 9+ runtimes use MethodHandles.privateLookupIn()
        // above.
        return PRIVATE_LOOKUPS.computeIfAbsent(clazz, Unchecked.function(PrivateLookupInKludge::getConstructorLookup));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static MethodHandles.Lookup getConstructorLookup(Class<?> type) throws ReflectiveOperationException {
        Constructor<Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);

        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }

        return constructor.newInstance(type, ANY_ACCESS);
    }
}
