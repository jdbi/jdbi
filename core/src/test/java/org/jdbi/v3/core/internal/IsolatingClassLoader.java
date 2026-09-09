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

import java.io.IOException;
import java.io.InputStream;

/**
 * Defines every class in one package itself from the test class path and delegates all other classes,
 * including Jdbi, to the parent. This mirrors a plugin loader (Paper, OSGi, application servers): the
 * parent, which is Jdbi's own loader, can not resolve the isolated classes by name, only the child can.
 * <p>
 * Nothing in the isolated package may be referenced from a test directly, or the parent loader would
 * define it first.
 */
public final class IsolatingClassLoader extends ClassLoader {

    private final String isolatedPackagePrefix;

    /**
     * @param isolatedPackage the package whose classes this loader defines itself, including subpackages
     */
    public IsolatingClassLoader(String isolatedPackage) {
        super(IsolatingClassLoader.class.getClassLoader());
        this.isolatedPackagePrefix = isolatedPackage + '.';
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(isolatedPackagePrefix)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineIsolated(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> defineIsolated(String name) throws ClassNotFoundException {
        String resource = name.replace('.', '/') + ".class";
        try (InputStream in = getParent().getResourceAsStream(resource)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes = in.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
