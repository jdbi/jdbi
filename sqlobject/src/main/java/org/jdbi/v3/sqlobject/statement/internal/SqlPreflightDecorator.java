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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.AttachedExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.sqlobject.statement.SqlPreflight;

/**
 * Runs the {@link SqlPreflight} statements declared on a SQL Object type and/or method, on the same
 * {@code Handle}, immediately before the method's main statement.
 */
public class SqlPreflightDecorator implements ExtensionHandlerCustomizer {

    @Override
    public ExtensionHandler customize(ExtensionHandler handler, Class<?> extensionType, Method method) {
        // This customizer is registered on both @SqlPreflight and its repeatable container, on both the
        // method and the type. The extension framework may therefore apply it more than once for a single
        // method (e.g. when @SqlPreflight is present on both the type and the method). Collapse those into
        // a single wrapper: each invocation collects the full type-then-method set, so the first wins.
        if (handler instanceof PreflightHandler) {
            return handler;
        }

        final List<String> sqls = new ArrayList<>();
        for (SqlPreflight preflight : extensionType.getAnnotationsByType(SqlPreflight.class)) {
            sqls.add(preflight.value());
        }
        for (SqlPreflight preflight : method.getAnnotationsByType(SqlPreflight.class)) {
            sqls.add(preflight.value());
        }

        if (sqls.isEmpty()) {
            return handler;
        }

        return new PreflightHandler(handler, List.copyOf(sqls), new ParameterBinder(extensionType, method));
    }

    private static final class PreflightHandler implements ExtensionHandler {

        private final ExtensionHandler delegate;
        private final List<String> sqls;
        private final ParameterBinder binder;

        PreflightHandler(ExtensionHandler delegate, List<String> sqls, ParameterBinder binder) {
            this.delegate = delegate;
            this.sqls = sqls;
            this.binder = binder;
        }

        @Override
        public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
            final AttachedExtensionHandler boundDelegate = delegate.attachTo(config, target);
            return (HandleSupplier handleSupplier, Object... args) -> {
                final Handle handle = handleSupplier.getHandle();
                for (String sql : sqls) {
                    try (Update update = handle.createUpdate(sql)) {
                        // The method's arguments are bound to every preflight statement; a preflight that
                        // references only some (or none) of them must not fail the unused-binding check.
                        update.getConfig(SqlStatements.class).setUnusedBindingAllowed(true);
                        binder.apply(update, args);
                        update.execute();
                    }
                }
                return boundDelegate.invoke(handleSupplier, args);
            };
        }
    }
}
