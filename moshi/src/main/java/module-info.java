module org.jdbi.v3.moshi {

	exports org.jdbi.v3.moshi;

    requires transitive org.jdbi.v3.core;
    requires transitive org.jdbi.v3.json;
    requires transitive com.squareup.moshi;
    requires static okio;
}
