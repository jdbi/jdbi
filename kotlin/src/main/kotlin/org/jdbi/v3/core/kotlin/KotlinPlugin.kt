package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.spi.JdbiPlugin

class KotlinPlugin : JdbiPlugin {

    override fun customizeJdbi(dbi: Jdbi) {
        dbi.registerRowMapper(KotlinMapperFactory())
    }
}
