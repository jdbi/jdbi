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
package org.jdbi.v3.core.statement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

class DefineNamedBindingsStatementCustomizer implements StatementCustomizer {
    @Override
    public void beforeTemplating(PreparedStatement stmt, StatementContext ctx) {
        final Set<String> alreadyDefined = ctx.getAttributes().keySet();
        final Binding binding = ctx.getBinding();
        final SetNullHandler handler = new SetNullHandler();
        binding.getNames().stream()
            .filter(name -> !alreadyDefined.contains(name))
            .forEach(name -> binding.findForName(name, ctx).ifPresent(
                    a -> handler.define(name, a, ctx)));
    }

    private static class SetNullHandler implements InvocationHandler {
        private static final Map<Class<?>, Object> DEFAULT_VALUES = new IdentityHashMap<>();
        static {
            DEFAULT_VALUES.put(boolean.class, false);
            DEFAULT_VALUES.put(char.class, '\u0000');
            DEFAULT_VALUES.put(byte.class, (byte) 0);
            DEFAULT_VALUES.put(short.class, (short) 0);
            DEFAULT_VALUES.put(int.class, 0);
            DEFAULT_VALUES.put(long.class, 0f);
            DEFAULT_VALUES.put(float.class, 0L);
            DEFAULT_VALUES.put(double.class, 0d);
        }

        private final PreparedStatement fakeStmt = (PreparedStatement)
                Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {PreparedStatement.class}, this);
        private boolean setNull;
        private boolean setCalled;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
            if (method.getName().equals("unwrap")
                && args.length == 1
                && method.getParameterTypes()[0].equals(Class.class)) {
                throw new SQLException("The current implementation of DefineNamedBindings is incompatible with "
                    + "arguments that rely on java.sql.Wrapper.unwrap(Class<?>)");
            }

            if (method.getName().startsWith("set")) {
                setCalled = true;
                boolean argNull = args.length > 1 && args[1] == null;
                setNull = argNull || "setNull".equals(method.getName());
            }

            return DEFAULT_VALUES.get(method.getReturnType());
        }

        void define(String name, Argument arg, StatementContext ctx) {
            setNull = false;
            setCalled = false;
            Unchecked.runnable(() -> arg.apply(1, fakeStmt, ctx)).run();
            if (setCalled) {
                ctx.define(name, !setNull);
            }
        }
    }
}
