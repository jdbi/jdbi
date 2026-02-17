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
package org.jdbi.sqlite3;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.assertj.core.api.Assertions;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.result.ResultSetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestUrls {

    Handle handle;

    @BeforeEach
    public void setUp() {
        Jdbi jdbi = Jdbi.create("jdbc:sqlite::memory:");
        jdbi.installPlugin(new SQLitePlugin());
        handle = jdbi.open();
        handle.useTransaction(h -> h.execute("CREATE TABLE foo(url URL);"));
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testInsertUrlSuccessful() throws MalformedURLException {
        String urlString = "file:/my-url";
        URL url = URI.create(urlString).toURL();

        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bind("url", url)
                .execute();

        URL actualUrl = handle.createQuery("SELECT url FROM foo").mapTo(URL.class).one();
        Assertions.assertThat(actualUrl).hasToString(urlString);
    }

    @Test
    public void testMapNullUrlThrowsException() {
        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bind("url", ((URL) null))
                .execute();

        assertThatThrownBy(() -> handle.createQuery("SELECT url FROM foo").mapTo(URL.class).one())
            .isInstanceOf(ResultSetException.class);
    }

    @Test
    public void testInsertUrlUsingBindByType() throws MalformedURLException {
        URL url = URI.create("file:/my-file").toURL();

        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bindByType("url", url, URL.class)
                .execute();

        URL dbUrl = handle.createQuery("SELECT * FROM foo").mapTo(URL.class).one();
        Assertions.assertThat(dbUrl).hasToString(url.toString());
    }

    @Test
    public void testInsertNullUrlUsingBindByType() {
        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bindByType("url", null, URL.class)
                .execute();

        assertThatThrownBy(() -> handle.createQuery("SELECT url FROM foo").mapTo(URL.class).one())
            .isInstanceOf(ResultSetException.class);
    }

}
