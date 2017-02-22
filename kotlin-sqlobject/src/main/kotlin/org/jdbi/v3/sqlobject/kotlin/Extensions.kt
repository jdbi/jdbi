package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import kotlin.reflect.KClass

fun <T : Any> Jdbi.onDemand(sqlObjectType: KClass<T>): T {
    return this.onDemand(sqlObjectType.java)
}

// inline fun <reified T: Any> DBI.onDemand(): T {
//    return this.onDemand(T::class)
// }

fun <T : Any> Handle.attach(sqlObjectType: KClass<T>): T {
    return this.attach(sqlObjectType.java)
}

// inline fun <reified T: Any> Handle.attach(): T {
//    return this.attach(T::class)
// }

