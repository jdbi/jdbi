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

import org.jdbi.v3.core.kotlin.isKotlinClass
import org.jdbi.v3.sqlobject.Handler
import org.jdbi.v3.sqlobject.HandlerFactory
import java.lang.reflect.Method
import java.util.*

class KotlinDefaultMethodHandlerFactory : HandlerFactory {

    private val implsClassCache = Collections.synchronizedMap(WeakHashMap<Class<*>, Map<MethodKey, Method>>())

    override fun buildHandler(sqlObjectType: Class<*>, method: Method): Optional<Handler> {
        val implementation = getImplementation(sqlObjectType, method) ?: return Optional.empty()

        return Optional.of(Handler { t, a, h -> implementation.invoke(null, *(listOf(t).plus(a).toTypedArray())) })

    }

    fun getImplementation(type: Class<*>, method: Method): Method? {
        if (!type.isKotlinClass()) return null

        val implMethods: Map<MethodKey, Method> = implsClassCache.computeIfAbsent(type) {
            findImplClass(it)?.methods?.associateBy { MethodKey(it.name, it.parameters.map { p -> p.type }, it.returnType) } ?: emptyMap()
        }!!

        return findImplMethod(type, method, implMethods)
    }

    private fun findImplMethod(type: Class<*>, method: Method, implMethods: Map<MethodKey, Method>): Method? {
        //default method is generated as static method that takes target interface as first parameter
        val paramTypes = listOf(type) + method.parameters.map { it.type }

        return implMethods[MethodKey(method.name, paramTypes, method.returnType)]
    }

    private fun findImplClass(type: Class<*>): Class<*>? = type.classes.find { it.simpleName == "DefaultImpls" }
}

data class MethodKey(val name: String, val paramTypes: List<Class<*>>, val returnType: Class<*>)

@Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private inline fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, crossinline mappingFunction: (K) -> V?): V?
        = (this as java.util.Map<K, V>).computeIfAbsent(key, java.util.function.Function { mappingFunction(it) })
