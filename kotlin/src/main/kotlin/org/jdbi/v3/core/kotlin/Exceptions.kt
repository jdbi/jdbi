package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.JdbiException


class KotlinMemberAccessException : JdbiException {

    constructor(string: String, throwable: Throwable) : super(string, throwable) {}

    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String) : super(message) {}

    companion object {
        private val serialVersionUID = 1L
    }
}

class NoSuchColumnMapperException : JdbiException {
    constructor(string: String, throwable: Throwable) : super(string, throwable) {}

    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String) : super(message) {}

    companion object {
        private val serialVersionUID = 1L
    }
}
