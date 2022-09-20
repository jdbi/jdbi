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
@file:Suppress("TooManyFunctions")

package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.config.Configurable
import org.jdbi.v3.core.config.JdbiConfig
import org.jdbi.v3.core.extension.ExtensionCallback
import org.jdbi.v3.core.extension.ExtensionConsumer
import org.jdbi.v3.core.kotlin.internal.KotlinPropertyArguments
import org.jdbi.v3.core.qualifier.Qualifier
import org.jdbi.v3.core.result.ResultBearing
import org.jdbi.v3.core.result.ResultIterable
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.meta.Beta
import java.util.function.Consumer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

private const val METADATA_FQ_NAME = "kotlin.Metadata"

fun Class<*>.isKotlinClass() = this.annotations.singleOrNull { it.annotationClass.java.name == METADATA_FQ_NAME } != null

internal fun KClass<*>.simpleName(): String = this.simpleName ?: this::java.javaClass.simpleName

/** Convenience helper to use KClass<T> for config lookup. */
fun <T : JdbiConfig<T>>StatementContext.getConfig(kClass: KClass<T>): T = this.getConfig(kClass.java)

inline fun <reified T : Any> ResultBearing.mapTo(): ResultIterable<T> = this.mapTo(T::class.java)

inline fun <O : Any> ResultIterable<O>.useSequence(block: (Sequence<O>) -> Unit) {
    this.iterator().use {
        block(it.asSequence())
    }
}

@Beta
fun <This : SqlStatement<This>> SqlStatement<This>.bindKotlin(name: String, obj: Any): This = this.bindNamedArgumentFinder(KotlinPropertyArguments(obj, name))

@Beta
fun <This : SqlStatement<This>> SqlStatement<This>.bindKotlin(obj: Any): This = this.bindNamedArgumentFinder(KotlinPropertyArguments(obj))

/**
 * Convenience method for {@link Configurable#configure} using Kotlin class syntax.
 */
@Beta
fun <This : Configurable<This>, C : JdbiConfig<C>> Configurable<This>.configure(configClass: KClass<C>, configurer: Consumer<C>): This =
    this.configure(configClass.java, configurer)

/**
 * A convenience method which opens an extension of the given type, yields it to a callback, and returns the result
 * of the callback. A handle is opened if needed by the extension, and closed before returning to the caller.
 *
 * @param extensionType the type of extension.
 * @param callback      a callback which will receive the extension.
 * @param <R> the return type
 * @param <E> the extension type
 * @param <X> the exception type optionally thrown by the callback
 * @return the value returned by the callback.
 * @throws org.jdbi.v3.core.extension.NoSuchExtensionException if no [org.jdbi.v3.core.extension.ExtensionFactory]
 * is registered which supports the given extension type.
 * @throws X                        if thrown by the callback.
 */
fun <E : Any, R, X : Exception> Jdbi.withExtension(extensionType: KClass<E>, callback: ExtensionCallback<R, E, X>): R =
    withExtension(extensionType.java, callback)

/**
 * A convenience method which opens an extension of the given type, and yields it to a callback. A handle is opened
 * if needed by the extention, and closed before returning to the caller.
 *
 * @param extensionType the type of extension
 * @param callback      a callback which will receive the extension
 * @param <E>           the extension type
 * @param <X>           the exception type optionally thrown by the callback
 * @throws org.jdbi.v3.core.extension.NoSuchExtensionException if no [org.jdbi.v3.core.extension.ExtensionFactory]
 * is registered which supports the given extension type.
 * @throws X                        if thrown by the callback.
 */
fun <E : Any, X : Exception> Jdbi.useExtension(extensionType: KClass<E>, callback: ExtensionConsumer<E, X>) {
    useExtension(extensionType.java, callback)
}

/**
 * Returns the set of qualifying annotations on the given Kotlin elements.
 * @param elements the annotated element. Null elements are ignored.
 * @return the set of qualifying annotations on the given elements.
 */
fun getQualifiers(vararg elements: KAnnotatedElement?): Set<Annotation> {
    return elements.filterNotNull()
        .flatMap { element -> element.annotations }
        .filter { anno -> anno.annotationClass.findAnnotation<Qualifier>() != null }
        .toSet()
}
