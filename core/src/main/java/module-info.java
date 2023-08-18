/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
module jdbi3.core {
    requires io.leangen.geantyref;
    requires java.desktop;

    requires transitive java.sql;

    requires static com.google.errorprone.annotations;
    requires static jakarta.annotation;
    requires static org.antlr.antlr4.runtime;
    requires static org.checkerframework.checker.qual;
    requires static org.slf4j;

    exports org.jdbi.v3.core;
    exports org.jdbi.v3.core.annotation;
    exports org.jdbi.v3.core.argument;
    exports org.jdbi.v3.core.array;
    exports org.jdbi.v3.core.async;
    exports org.jdbi.v3.core.cache;
    exports org.jdbi.v3.core.codec;
    exports org.jdbi.v3.core.collector;
    exports org.jdbi.v3.core.config;
    exports org.jdbi.v3.core.enums;
    exports org.jdbi.v3.core.extension;
    exports org.jdbi.v3.core.extension.annotation;
    exports org.jdbi.v3.core.generic;
    exports org.jdbi.v3.core.h2;
    exports org.jdbi.v3.core.interceptor;
    exports org.jdbi.v3.core.locator;
    exports org.jdbi.v3.core.mapper;
    exports org.jdbi.v3.core.mapper.freebuilder;
    exports org.jdbi.v3.core.mapper.immutables;
    exports org.jdbi.v3.core.mapper.reflect;
    exports org.jdbi.v3.core.qualifier;
    exports org.jdbi.v3.core.result;
    exports org.jdbi.v3.core.spi;
    exports org.jdbi.v3.core.statement;
    exports org.jdbi.v3.core.transaction;
    exports org.jdbi.v3.meta;

    exports org.jdbi.v3.core.internal to jdbi3.testing;
    exports org.jdbi.v3.core.cache.internal to jdbi3.caffeine.cache;

    uses org.jdbi.v3.core.spi.JdbiPlugin;
}
