module org.jdbi.v3.caffeine {

	exports org.jdbi.v3.cache.caffeine;

    requires transitive org.jdbi.v3.core;
    requires transitive com.github.benmanes.caffeine;
}
