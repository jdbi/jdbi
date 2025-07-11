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
package org.jdbi.v3.examples.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jdbi.v3.core.extension.AttachedExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.v3.core.statement.Query;

/**
 * Sample code for per-statement authentication with the database.<br>
 * <p>
 * Every Dao needs to be annotated with the {@link UseAuthentication} annotation. The sample code requires every method on those dao objects to take a
 * {@link AuthContext} object as its first parameter. This parameter should <b>not</b> be used by the DAO method itself. The annotation will instantiate the
 * {@link AuthenticationHandler} which is implemented as a {@link ExtensionHandlerCustomizer} for the Jdbi Extension Framework. This handler could either call
 * out to an external authentication handler or (as in this example) run an inline query using the supplied handler and parameters and make an authentication
 * decision.<br>
 * <p>
 * Note that the handler will be instantiated separately for every method in the annotated types. Accessing an external authentication service will require
 * injection of a static singleton into the class.
 */
public class AuthenticationExample {

    /**
     * Authentication context. Only contains login and credential. This is demo code, do <b>NOT</b> use this in any 'real' scenario.
     */
    public static class AuthContext {
        private final String username;
        private final String password;

        public AuthContext(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    /**
     * Authenticaion Annotation. When put on a Dao object, it will apply to all methods in the Dao. If put on a method, it will only apply to the method
     * itself.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE }) // <1>
    @UseExtensionHandlerCustomizer(AuthenticationHandler.class)
    public @interface UseAuthentication {}

    /**
     * The authentication handler.
     */
    public static class AuthenticationHandler implements ExtensionHandlerCustomizer {
        @Override
        public ExtensionHandler customize(ExtensionHandler handler, Class<?> extensionType, Method method) {
            return (config, target) -> {
                AttachedExtensionHandler delegate = handler.attachTo(config, target);
                return (handleSupplier, args) -> {
                    // Start of the authentication code

                    // check whether a context was passed as the first argument. Bail out if not.
                    if (args == null || args.length == 0 || !(args[0] instanceof AuthContext)) {
                        throw new IllegalArgumentException("First argument must be an AuthContext");
                    }

                    // Authentication "check". This sample code does an inline check against the database.
                    // This is not secure code or intended to be used in any real scenario. This is intended
                    // to show how to make database calls from inside the extension handler customizer.

                    AuthContext authContext = (AuthContext) args[0];

                    try (Query query = handleSupplier.getHandle().createQuery("SELECT 1 FROM users WHERE name = :username AND password = :password")) {
                        var valid = query
                                .bind("username", authContext.getUsername())
                                .bind("password", authContext.getPassword())
                                .mapTo(Integer.class).findOne();
                        if (valid.isEmpty()) {
                            throw new IllegalStateException("Invalid credentials");
                        }
                    }

                    // when successful, call the delegated handler.

                    return delegate.invoke(handleSupplier, args);
                };
            };
        }
    }
}
