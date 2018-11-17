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
package org.jdbi.v3.core.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class AnnotationFactory {
    private AnnotationFactory() {}

    public static <T extends Annotation> T create(Class<T> annotationType) {
        @SuppressWarnings("unchecked")
        T annotation = (T) Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class[]{annotationType},
            getInvocationHandler(annotationType));
        return annotation;
    }

    private static <T extends Annotation> InvocationHandler getInvocationHandler(Class<T> annotationType) {
        return (proxy, method, args) -> {
            String name = method.getName();
            if ("annotationType".equals(name) && method.getParameterCount() == 0) {
                return annotationType;
            }

            if ("equals".equals(name) && method.getParameterCount() == 1
                && Object.class.equals(method.getParameterTypes()[0])) {
                Annotation that = (Annotation) args[0];
                return annotationType.equals(that.annotationType());
            }

            if ("hashCode".equals(name) && method.getParameterCount() == 0) {
                return 0;
            }

            if ("toString".equals(name) && method.getParameterCount() == 0) {
                return "@" + annotationType.getCanonicalName() + "()";
            }

            throw new IllegalStateException("Unknown method " + method + " for annotation type " + annotationType);
        };
    }
}
