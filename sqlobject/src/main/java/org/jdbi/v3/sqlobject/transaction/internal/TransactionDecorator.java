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
package org.jdbi.v3.sqlobject.transaction.internal;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public class TransactionDecorator implements ExtensionHandlerCustomizer {
    @Override
    public ExtensionHandler customize(ExtensionHandler delegate, Class<?> sqlObjectType, Method method) {
        final Transaction txnAnnotation = Stream.of(method, sqlObjectType)
                .map(ae -> ae.getAnnotation(Transaction.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new TransactionException("No @Transaction annotation found"));
        final TransactionIsolationLevel isolation = txnAnnotation.value();
        final boolean readOnly = txnAnnotation.readOnly();

        return (handleSupplier, target, args) -> {
            Handle handle = handleSupplier.getHandle();

            if (handle.isInTransaction() && handle.isReadOnly() && !readOnly) {
                throw new TransactionException("Tried to execute a nested @Transaction(readOnly=false) "
                        + "inside a readOnly transaction");
            }

            HandleCallback<Object, Exception> callback = transactionHandle -> delegate.invoke(handleSupplier, target, args);

            final boolean flipReadOnly = readOnly != handle.isReadOnly();
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
        };
    }
}
