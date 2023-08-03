module org.jdbi.v3.jackson2 {

	exports org.jdbi.v3.jackson2;

    requires transitive org.jdbi.v3.core;
    requires transitive org.jdbi.v3.json;
    requires transitive com.fasterxml.jackson.databind;
}
