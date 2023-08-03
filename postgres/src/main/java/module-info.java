module org.jdbi.v3.postgres {

	exports org.jdbi.v3.postgres;
	exports org.jdbi.v3.postgres.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive org.postgresql.jdbc;
    requires transitive org.jdbi.v3.json;
}
