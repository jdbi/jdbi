module org.jdbi.v3.guava {

	exports org.jdbi.v3.guava;
	exports org.jdbi.v3.guava.codec;

    requires transitive org.jdbi.v3.core;
    requires transitive com.google.common;
}
