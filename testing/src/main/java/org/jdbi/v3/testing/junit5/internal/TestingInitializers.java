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
package org.jdbi.v3.testing.junit5.internal;

import org.jdbi.v3.testing.junit5.JdbiExtensionInitializer;

/**
 * This class is used in the various Jdbi unit tests. It is not intended for any tests outside the Jdbi code.
 */
public final class TestingInitializers {

    private TestingInitializers() {
        throw new AssertionError("TestingInitializers can not be instantiated");
    }

    public static JdbiExtensionInitializer something() {
        return (ds, h) -> h.execute("create table something (id identity primary key, name varchar(50), integerValue integer, intValue integer)");
    }

    public static JdbiExtensionInitializer users() {
        return (ds, h) -> h.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR)");
    }

    public static JdbiExtensionInitializer usersWithData() {
        return (ds, h) -> {
            users().initialize(ds, h);
            h.execute("INSERT INTO users VALUES (1, 'Alice')");
            h.execute("INSERT INTO users VALUES (2, 'Bob')");
        };
    }
}
