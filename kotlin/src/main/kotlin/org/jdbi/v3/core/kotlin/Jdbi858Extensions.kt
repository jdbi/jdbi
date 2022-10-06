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
package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.HandleCallback
import org.jdbi.v3.core.HandleConsumer
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.extension.ExtensionCallback
import org.jdbi.v3.core.extension.ExtensionConsumer
import org.jdbi.v3.core.transaction.TransactionIsolationLevel
import org.jdbi.v3.meta.Alpha
import kotlin.reflect.KClass

// The extensions in this file were created in response to these issues :
// https://github.com/jdbi/jdbi/issues/858
// https://youtrack.jetbrains.com/issue/KT-5464

// These extensions are temporary and will be removed when Kotlin's type inference
// is improved and this isn't an issue anymore.

/**
 * Temporary extension function for [Jdbi.withHandle].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.withHandle]
 */
@Alpha
inline fun <R> Jdbi.withHandleUnchecked(crossinline block: (Handle) -> R): R = withHandle(HandleCallback<R, RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.useHandle].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.useHandle]
 */
@Alpha
inline fun Jdbi.useHandleUnchecked(crossinline block: (Handle) -> Unit): Unit = useHandle(HandleConsumer<RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.inTransaction].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.inTransaction]
 */
@Alpha
inline fun <R> Jdbi.inTransactionUnchecked(crossinline block: (Handle) -> R): R = inTransaction(HandleCallback<R, RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.useTransaction].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.useTransaction]
 */
@Alpha
inline fun Jdbi.useTransactionUnchecked(crossinline block: (Handle) -> Unit): Unit =
    useTransaction(HandleConsumer<RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.inTransaction].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.inTransaction]
 */
@Alpha
inline fun <R> Jdbi.inTransactionUnchecked(level: TransactionIsolationLevel, crossinline block: (Handle) -> R): R =
    inTransaction(level, HandleCallback<R, RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.useTransaction].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.useTransaction]
 */
@Alpha
inline fun Jdbi.useTransactionUnchecked(level: TransactionIsolationLevel, crossinline block: (Handle) -> Unit): Unit =
    useTransaction(level, HandleConsumer<RuntimeException> { handle -> block(handle) })

/**
 * Temporary extension function for [Jdbi.withExtension].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.withExtension]
 */
@Alpha
inline fun <E, R> Jdbi.withExtensionUnchecked(extensionType: Class<E>, crossinline callback: (E) -> R): R =
    withExtension(extensionType, ExtensionCallback<R, E, RuntimeException> { dao -> callback(dao) })

/**
 * Temporary extension function for [Jdbi.withExtension].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.withExtension]
 */
@Alpha
inline fun <E : Any, R> Jdbi.withExtensionUnchecked(extensionType: KClass<E>, crossinline callback: (E) -> R): R =
    withExtension(extensionType, ExtensionCallback<R, E, RuntimeException> { dao -> callback(dao) })

/**
 * Temporary extension function for [Jdbi.useExtension].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.useExtension]
 */
@Alpha
inline fun <E> Jdbi.useExtensionUnchecked(extensionType: Class<E>, crossinline callback: (E) -> Unit): Unit =
    useExtension(extensionType, ExtensionConsumer<E, RuntimeException> { dao -> callback(dao) })

/**
 * Temporary extension function for [Jdbi.useExtension].
 *
 * This function WILL be deprecated and removed when not needed anymore.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/858">Github issue</a>
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-5464">Kotlin issue</a>
 * @see [Jdbi.useExtension]
 */
@Alpha
inline fun <E : Any> Jdbi.useExtensionUnchecked(extensionType: KClass<E>, crossinline callback: (E) -> Unit): Unit =
    useExtension(extensionType, ExtensionConsumer<E, RuntimeException> { dao -> callback(dao) })
