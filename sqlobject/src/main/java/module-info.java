module org.jdbi.v3.sqlobject {

	exports org.jdbi.v3.sqlobject;
	exports org.jdbi.v3.sqlobject.config;
	exports org.jdbi.v3.sqlobject.config.internal;
	exports org.jdbi.v3.sqlobject.customizer;
	exports org.jdbi.v3.sqlobject.customizer.internal;
	exports org.jdbi.v3.sqlobject.internal;
	exports org.jdbi.v3.sqlobject.locator;
	exports org.jdbi.v3.sqlobject.locator.internal;
	exports org.jdbi.v3.sqlobject.statement;
	exports org.jdbi.v3.sqlobject.statement.internal;
	exports org.jdbi.v3.sqlobject.transaction;
	exports org.jdbi.v3.sqlobject.transaction.internal;

    requires transitive org.jdbi.v3.core;
}
