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
package jdbi.doc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCollections {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();

        handle.execute("CREATE TABLE films (title VARCHAR(255), genre VARCHAR(255), release_year INT, id INT PRIMARY KEY)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Shawshank Redemption', 'Drama', 1994, 1)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Godfather', 'Crime', 1972, 2)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Dark Knight', 'Action', 2008, 3)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Godfather, Part II', 'Crime', 1974, 4)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('12 Angry Men', 'Drama', 1957, 5)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('Schindlers List', 'Biography', 1993, 6)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Lord of the Rings: Return of the King', 'Fantasy', 2003, 7)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('Pulp Ficton', 'Drama', 1994, 8)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Lord of the Rings: The Fellowship of the Rings', 'Fantasy', 2001, 9)");
        handle.execute("INSERT INTO films (title, genre, release_year, id) VALUES ('The Good, The Bad and the Ugly', 'Western', 1966, 10)");
    }

    @Test
    public void testList() {
        // tag::list[]
        List<String> names = this.handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .list();
        // end::list[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testSet() {
        // tag::set[]
        Set<String> names = handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .set();
        // end::set[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void test() {
        // tag::into-list[]
        List<String> names = handle
                .registerCollector(List.class, Collectors.toCollection(ArrayList::new))
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .collectIntoList();
        // end::into-list[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testIntoSet() {
        // tag::into-set[]
        Set<String> names = handle
                .registerCollector(Set.class, Collectors.toCollection(LinkedHashSet::new))
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .collectIntoSet();
        // end::into-set[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testCollectInto() {
        // tag::collect-into[]
        List<String> names = handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .collectInto(List.class); // <1>
        // end::collect-into[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testCollectIntoGeneric() {
        // tag::collect-into-generic[]
        List<String> names = handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .collectInto(GenericTypes.parameterizeClass(List.class, String.class)); // <2>
        // end::collect-into-generic[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testCollect() {
        // tag::collect[]
        Set<String> names = handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .collect(Collectors.toSet());
        // end::collect[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testToCollection() {
        // tag::to-collection[]
        Set<String> names = handle
                .createQuery("SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .toCollection(() -> new LinkedHashSet<>());
        // end::to-collection[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testMapTo() {
        // tag::to-map[]
        Map<Integer, String> names = handle
                .registerRowMapper(Movie.class, ConstructorMapper.of(Movie.class))
                .createQuery("SELECT * FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(Movie.class)
                .collectToMap(Movie::id, Movie::title);
        // end::to-map[]

        assertThat(names)
                .hasSize(1)
                .containsEntry(3, "The Dark Knight");
    }

    @Test
    public void testUseStream() {
        // tag::use-stream[]
        List<String> names = new ArrayList<>();
        handle.createQuery(
                        "SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .useStream(stream -> stream.forEach(names::add)); // <1>
        // end::use-stream[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }

    @Test
    public void testStream() {
        // tag::stream[]
        List<String> names = new ArrayList<>();
        try (Stream<String> stream = handle.createQuery(
                        "SELECT title FROM films WHERE genre = :genre ORDER BY title")
                .bind("genre", "Action")
                .mapTo(String.class)
                .stream()) { // <2>

            stream.forEach(names::add);
        }
        // end::stream[]

        assertThat(names)
                .hasSize(1)
                .containsExactly("The Dark Knight");
    }


    @Test
    public void testIntResult() {
        // tag::int-result[]
        int releaseDate = handle
                .createQuery("SELECT release_year FROM films WHERE title = :title")
                .bind("title", "The Dark Knight")
                .mapTo(Integer.class)
                .one();
        // end::int-result[]

        assertThat(releaseDate).isEqualTo(2008);
    }

    @Test
    public void testMovieResult() {
        // tag::movie-result[]
        Movie movie = handle
                .registerRowMapper(Movie.class, ConstructorMapper.of(Movie.class))
                .createQuery("SELECT * FROM films WHERE id = :id")
                .bind("id", 1)
                .mapTo(Movie.class)
                .one();
        // end::movie-result[]

        assertThat(movie).isEqualTo(new Movie(1, "The Shawshank Redemption", "Drama", 1994));
    }


    public static class Movie {

        private final int id;
        private final String title;
        private final String genre;
        private final int releaseYear;

        public Movie(int id, String title, String genre, int releaseYear) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.releaseYear = releaseYear;
        }

        public int id() {
            return id;
        }

        public String title() {
            return title;
        }

        public String genre() {
            return genre;
        }

        public int releaseYear() {
            return releaseYear;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Movie movie = (Movie) o;
            return id == movie.id && releaseYear == movie.releaseYear && Objects.equals(title, movie.title) && Objects.equals(genre, movie.genre);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, title, genre, releaseYear);
        }
    }
}
