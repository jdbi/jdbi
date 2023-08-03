module org.jdbi.v3.freemarker {

	exports org.jdbi.v3.freemarker;
	exports org.jdbi.v3.freemarker.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive freemarker;
    requires transitive org.jdbi.v3.sqlobject;
}
