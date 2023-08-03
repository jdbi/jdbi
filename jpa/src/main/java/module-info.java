module org.jdbi.v3.jpa {

	exports org.jdbi.v3.jpa;
	exports org.jdbi.v3.jpa.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive org.jdbi.v3.sqlobject;
    requires transitive persistence.api;
}
