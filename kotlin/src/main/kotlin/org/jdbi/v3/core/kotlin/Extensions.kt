package org.jdbi.v3.core.kotlin


import org.jdbi.v3.core.result.ResultBearing
import org.jdbi.v3.core.result.ResultIterable
import org.jdbi.v3.core.statement.Query
import kotlin.reflect.KClass

private val metadataFqName = "kotlin.Metadata"

fun Class<*>.isKotlinClass(): Boolean {
    return this.annotations.singleOrNull { it.annotationClass.java.name == metadataFqName } != null
}

fun <T : Any> ResultBearing.map(toClass: KClass<T>): ResultIterable<T> {
    return this.map(KotlinMapper(toClass.java))
}

fun <O : Any> ResultIterable<O>.useSequence(block: (Sequence<O>) -> Unit): Unit {
    this.iterator().use {
        block(it.asSequence())
    }
}

fun <T : Any> Query.useSequence(toClass: KClass<T>, block: (Sequence<T>) -> Unit): Unit {
    this.map(toClass).iterator().use {
        block(it.asSequence())
    }
}
