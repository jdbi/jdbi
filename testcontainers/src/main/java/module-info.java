module org.jdbi.v3.testcontainers {

	exports org.jdbi.v3.testing.junit5.tc;

    requires transitive org.jdbi.v3.core;
    requires transitive com.zaxxer.hikari;
    requires transitive org.junit.jupiter.api;
  	requires transitive jdbc;
  	requires transitive org.jdbi.v3.testing;
}
