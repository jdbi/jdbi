module org.jdbi.v3.guice {

	exports org.jdbi.v3.guice;
	exports org.jdbi.v3.guice.internal;

    requires transitive org.jdbi.v3.core;
    requires transitive com.google.guice;
    requires transitive jakarta.inject;
    requires transitive org.jdbi.v3.guava;
    requires static javax.inject;
}
