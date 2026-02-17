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
package org.jdbi.sqlobject.transaction.internal;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.HandleCallback;
import org.jdbi.core.extension.AttachedExtensionHandler;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.transaction.TransactionException;
import org.jdbi.core.transaction.TransactionIsolationLevel;
import org.jdbi.sqlobject.transaction.Transaction;

public class TransactionDecorator implements ExtensionHandlerCustomizer {
    @Override
    public ExtensionHandler customize(ExtensionHandler delegate, Class<?> sqlObjectType, Method method) {
        final Transaction txnAnnotation = Stream.of(method, sqlObjectType)
                .map(ae -> ae.getAnnotation(Transaction.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new TransactionException("No @Transaction annotation found"));

        return (config, target) -> {
            AttachedExtensionHandler boundDelegate = delegate.attachTo(config, target);
            return new InTransaction(txnAnnotation, boundDelegate);
        };
    }

    static class InTransaction implements AttachedExtensionHandler {
        private final AttachedExtensionHandler boundDelegate;
        private final boolean readOnly;
        private final TransactionIsolationLevel isolation;

        InTransaction(Transaction txnAnnotation, AttachedExtensionHandler boundDelegate) {
            this.boundDelegate = boundDelegate;
            isolation = txnAnnotation.value();
            readOnly = txnAnnotation.readOnly();
        }

        @Override
        public Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception {
            Handle handle = handleSupplier.getHandle();

            if (handle.isInTransaction() && handle.isReadOnly() && !readOnly) {
                throw new TransactionException("Tried to execute a nested @Transaction(readOnly=false) "
                        + "inside a readOnly transaction");
            }

            var callback = new HandleCallback<Object, Exception>() {
                @Override
                public Object withHandle(Handle handle) throws Exception {
                    return boundDelegate.invoke(handleSupplier, args);
                }
            };

            boolean flipReadOnly = readOnly != handle.isReadOnly();
            if (flipReadOnly) {
                handle.setReadOnly(readOnly);
            }

            try {
                return handle.inTransaction(isolation, callback);
            } finally {
                if (flipReadOnly) {
                    handle.setReadOnly(!readOnly);
                }
            }
        }
    }
}
