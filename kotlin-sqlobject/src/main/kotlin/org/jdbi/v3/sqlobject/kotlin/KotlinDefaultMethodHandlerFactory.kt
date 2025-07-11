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
package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.core.extension.AttachedExtensionHandler
import org.jdbi.v3.core.extension.ExtensionHandler
import org.jdbi.v3.core.extension.ExtensionHandlerFactory
import org.jdbi.v3.core.kotlin.isKotlinClass
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Collections
import java.util.Optional
import java.util.WeakHashMap

class KotlinDefaultMethodHandlerFactory : ExtensionHandlerFactory {

    private val implsClassCache = Collections.synchronizedMap(WeakHashMap<Class<*>, Map<MethodKey, Method>>())

    override fun accepts(extensionType: Class<*>?, method: Method?): Boolean = extensionType?.isKotlinClass() ?: false

    override fun createExtensionHandler(sqlObjectType: Class<*>, method: Method): Optional<ExtensionHandler> {
        val implementation = getImplementation(sqlObjectType, method) ?: return Optional.empty()

        return Optional.of(
            ExtensionHandler { _, t ->
                AttachedExtensionHandler { _, a ->
                    @Suppress("SwallowedException")
                    try {
                        @Suppress("SpreadOperator")
                        implementation.invoke(null, t, *a)
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }
            }
        )
    }

    private fun getImplementation(extensionType: Class<*>, method: Method): Method? {
        if (!extensionType.isKotlinClass()) return null

        val implMethods: Map<MethodKey, Method> = implsClassCache.computeIfAbsent(extensionType) { type ->
            findImplClass(type)?.methods?.associateBy { MethodKey(it.name, it.parameters.map { p -> p.type }, it.returnType) } ?: emptyMap()
        }!!

        return findImplMethod(extensionType, method, implMethods)
    }

    private fun findImplMethod(type: Class<*>, method: Method, implMethods: Map<MethodKey, Method>): Method? {
        // default method is generated as static method that takes target interface as first parameter
        val paramTypes = listOf(type) + method.parameters.map { it.type }

        return implMethods[MethodKey(method.name, paramTypes, method.returnType)]
    }

    private fun findImplClass(type: Class<*>): Class<*>? = type.classes.find { it.simpleName == "DefaultImpls" }
}

data class MethodKey(val name: String, val paramTypes: List<Class<*>>, val returnType: Class<*>)
