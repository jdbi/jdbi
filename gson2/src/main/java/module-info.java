module org.jdbi.v3.gson2 {

	exports org.jdbi.v3.gson2;
	requires jdbi3.json;

    requires transitive org.jdbi.v3.core;
    requires transitive com.google.gson;
}
