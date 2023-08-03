module org.jdbi.v3.testing {

	exports org.jdbi.v3.testing;
	exports org.jdbi.v3.testing.junit5;
	exports org.jdbi.v3.testing.junit5.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive org.junit.jupiter.api;
	requires transitive junit;
    requires static transitive org.xerial.sqlitejdbc;
    requires static transitive otj.pg.embedded;
  	requires static transitive org.flywaydb.core;
	requires static transitive com.h2database;
  	requires static transitive org.postgresql.jdbc;
    requires static transitive de.softwareforge.testing.postgres;
}
