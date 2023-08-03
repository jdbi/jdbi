import org.jdbi.v3.core.spi.JdbiPlugin;

module org.jdbi.v3.core {

	exports org.jdbi.v3.core;
	exports org.jdbi.v3.core.annotation;
	exports org.jdbi.v3.core.annotation.internal;
	exports org.jdbi.v3.core.argument;
	exports org.jdbi.v3.core.argument.internal;
	exports org.jdbi.v3.core.argument.internal.strategies;
	exports org.jdbi.v3.core.array;
	exports org.jdbi.v3.core.async;
	exports org.jdbi.v3.core.cache;
	exports org.jdbi.v3.core.cache.internal;
	exports org.jdbi.v3.core.codec;
	exports org.jdbi.v3.core.collector;
	exports org.jdbi.v3.core.config;
	exports org.jdbi.v3.core.config.internal;
	exports org.jdbi.v3.core.enums;
	exports org.jdbi.v3.core.enums.internal;
	exports org.jdbi.v3.core.extension;
	exports org.jdbi.v3.core.extension.annotation;
	exports org.jdbi.v3.core.generic;
	exports org.jdbi.v3.core.h2;
	exports org.jdbi.v3.core.interceptor;
	exports org.jdbi.v3.core.internal;
	exports org.jdbi.v3.core.internal.exceptions;
	exports org.jdbi.v3.core.locator;
	exports org.jdbi.v3.core.locator.internal;
	exports org.jdbi.v3.core.mapper;
	exports org.jdbi.v3.core.mapper.freebuilder;
	exports org.jdbi.v3.core.mapper.immutables;
	exports org.jdbi.v3.core.mapper.reflect;
	exports org.jdbi.v3.core.mapper.reflect.internal;
	exports org.jdbi.v3.core.qualifier;
	exports org.jdbi.v3.core.result;
	exports org.jdbi.v3.core.result.internal;
	exports org.jdbi.v3.core.spi;
	exports org.jdbi.v3.core.statement;
	exports org.jdbi.v3.core.statement.internal;
	exports org.jdbi.v3.core.transaction;
	exports org.jdbi.v3.meta;

	requires transitive java.sql;
	requires transitive com.google.errorprone.annotations;
	requires transitive org.slf4j;
    requires transitive com.github.spotbugs.annotations;
    requires transitive io.leangen.geantyref;
    requires transitive org.antlr.antlr4.runtime;
    requires transitive java.desktop;

    uses JdbiPlugin;
}
