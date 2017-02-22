package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.spi.JdbiPlugin
import org.jdbi.v3.sqlobject.Handlers
import org.jdbi.v3.sqlobject.SqlObjects

class SqlObjectKotlinPlugin : JdbiPlugin {

    override fun customizeJdbi(dbi: Jdbi) {
        dbi.configure(SqlObjects::class.java, { c -> c.defaultParameterCustomizerFactory = KotlinSqlStatementCustomiserFactory() })
        dbi.configure(Handlers::class.java, { c -> c.register(KotlinDefaultMethodHandlerFactory()) })
    }
}
