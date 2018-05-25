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
package org.jdbi.v3.sqlite3;

import java.net.MalformedURLException;
import java.net.URL;
import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultSetException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestUrls {

    Handle handle;

    @Before
    public void setUp() throws Exception {
        Jdbi jdbi = Jdbi.create("jdbc:sqlite::memory:");
        jdbi.installPlugin(new SQLitePlugin());
        handle = jdbi.open();
        handle.useTransaction(handle -> handle.execute("CREATE TABLE foo(url URL);"));
    }

    @After
    public void tearDown() throws Exception {
        handle.close();
    }

    @Test
    public void testInsertUrlSuccessful() throws MalformedURLException {
        String googleString = "http://www.google.com";
        URL googleUrl = new URL(googleString);

        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bind("url", googleUrl)
                .execute();

        URL actualUrl = handle.createQuery("SELECT url FROM foo").mapTo(URL.class).findOnly();
        Assertions.assertThat(actualUrl).hasToString(googleUrl.toString());
    }

    @Test
    public void testMapNullUrlThrowsException() {
        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bind("url", ((URL) null))
                .execute();

        assertThatThrownBy(() -> handle.createQuery("SELECT url FROM foo").mapTo(URL.class).findOnly())
            .isInstanceOf(ResultSetException.class);
    }

    @Test
    public void testInsertUrlUsingBindByType() throws MalformedURLException {
        URL githubUrl = new URL("http://www.github.com");

        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bindByType("url", githubUrl, URL.class)
                .execute();

        URL dbUrl = handle.createQuery("SELECT * FROM foo").mapTo(URL.class).findOnly();
        Assertions.assertThat(dbUrl).hasToString(githubUrl.toString());
    }

    @Test
    public void testInsertNullUrlUsingBindByType() {
        handle.createUpdate("INSERT INTO foo VALUES (:url)")
                .bindByType("url", null, URL.class)
                .execute();

        assertThatThrownBy(() -> handle.createQuery("SELECT url FROM foo").mapTo(URL.class).findOnly())
            .isInstanceOf(ResultSetException.class);
    }

}
