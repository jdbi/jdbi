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
        private final PreparedStatement fakeStmt = (PreparedStatement)
                Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {PreparedStatement.class}, this);
        private boolean setNull;
        private boolean setCalled;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            setCalled = method.getName().startsWith("set");
            boolean argNull = args.length > 1 && args[1] == null;
            setNull = "setNull".equals(method.getName()) || argNull;
            return null;
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
