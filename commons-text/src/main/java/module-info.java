module org.jdbi.v3.commonstext {

	exports org.jdbi.v3.commonstext;
	exports org.jdbi.v3.commonstext.internal;
    requires transitive org.apache.commons.text;
    requires transitive org.jdbi.v3.core;
}
