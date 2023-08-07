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
module org.jdbi.v3.core.test {

	exports org.jdbi.v3.core.test;
	exports org.jdbi.v3.core.test.argument;
	exports org.jdbi.v3.core.test.cache.internal;
	exports org.jdbi.v3.core.test.codec;
	exports org.jdbi.v3.core.test.collector;
	exports org.jdbi.v3.core.test.config;
	exports org.jdbi.v3.core.test.extension;
	exports org.jdbi.v3.core.test.generic;
	exports org.jdbi.v3.core.test.h2;
	exports org.jdbi.v3.core.test.interceptor;
	exports org.jdbi.v3.core.test.internal;
	exports org.jdbi.v3.core.test.internal.exceptions;
	exports org.jdbi.v3.core.test.locator.internal;
	exports org.jdbi.v3.core.test.mapper;
	exports org.jdbi.v3.core.test.qualifier;
	exports org.jdbi.v3.core.test.statement;

    requires transitive org.jdbi.v3.core;
    requires transitive org.assertj.core;
    requires transitive org.junit.jupiter.api;
    requires transitive de.softwareforge.testing.postgres;
    requires com.google.common;
    requires static freebuilder;
  	requires org.immutables.value;
  	requires static jsr305;
  	requires static java.compiler;
}
