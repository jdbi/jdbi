/*
 * Copyright (C) 2005 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jdbi.v3.lib.internal.com_google_guava.guava.v21_0;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.jdbi.v3.core.internal.UtilityClassException;

import static org.jdbi.v3.lib.internal.com_google_guava.guava.v21_0.Preconditions.checkArgument;
import static org.jdbi.v3.lib.internal.com_google_guava.guava.v21_0.Preconditions.checkNotNull;

public class Reflection {
    private Reflection() {
        throw new UtilityClassException();
    }

    public static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
        checkNotNull(handler, "handler");
        checkArgument(interfaceType.isInterface(), "%s is not an interface", interfaceType);
        Object object =
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(), new Class<?>[] {interfaceType}, handler);
        return interfaceType.cast(object);
    }
}
