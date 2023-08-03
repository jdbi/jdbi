module org.jdbi.v3.generator {

	exports org.jdbi.v3.generator;
    requires transitive java.compiler;
    requires transitive com.squareup.javapoet;
    requires transitive org.jdbi.v3.core;
    requires transitive org.jdbi.v3.sqlobject;

    provides javax.annotation.processing.Processor with org.jdbi.v3.generator.GenerateSqlObjectProcessor;


}
