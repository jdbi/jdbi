module org.jdbi.v3.json {

	exports org.jdbi.v3.json;
	exports org.jdbi.v3.json.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive org.jdbi.v3.sqlobject;
    requires transitive persistence.api;
}
