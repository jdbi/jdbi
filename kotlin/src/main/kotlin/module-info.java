module org.jdbi.v3.kotlin {

	exports org.jdbi.v3.core.kotlin;
	exports org.jdbi.v3.core.kotlin.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive kotlin.reflect;
}
