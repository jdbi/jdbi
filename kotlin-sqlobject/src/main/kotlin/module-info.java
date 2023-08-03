module org.jdbi.v3.sqlobject.kotlin {

	exports org.jdbi.v3.sqlobject.kotlin;
	exports org.jdbi.v3.sqlobject.kotlin.internal;

    requires transitive org.jdbi.v3.sqlobject;
    requires transitive org.jdbi.v3.core;
    requires transitive kotlin.reflect;
}
