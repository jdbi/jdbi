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
package org.jdbi.v3.sqlobject.config;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisterImmutableMapper {
    @Rule
    public H2DatabaseRule rule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    Handle handle;

    @Before
    public void setUp() {
        handle = rule.getSharedHandle();

        handle.execute("create table books (" +
            "id integer not null, " +
            "name varchar not null, " +
            "bookshelf_id integer not null)");
        handle.execute("create table bookshelfs (" +
            "id integer not null, " +
            "name varchar not null)");

        handle.execute("insert into bookshelfs (id, name) values (?, ?)", 1, "bookshelf 1");
        handle.execute("insert into bookshelfs (id, name) values (?, ?)", 2, "bookshelf 2");

        handle.execute("insert into books (id, name, bookshelf_id) values (?, ?, ?)", 1, "book 1", 1);
        handle.execute("insert into books (id, name, bookshelf_id) values (?, ?, ?)", 2, "book 2", 1);
        handle.execute("insert into books (id, name, bookshelf_id) values (?, ?, ?)", 3, "book 3", 2);
        handle.execute("insert into books (id, name, bookshelf_id) values (?, ?, ?)", 4, "book 4", 2);
    }

    @Test
    public void registerImmutableMappers() {
        PrivateBookDao dao = handle.attach(PrivateBookDao.class);

        assertThat(dao.listBookshelfs().stream().map(Bookshelf::id)).containsExactly(1, 2);

        assertThat(dao.getArticleWithComments(0)).isEmpty();
        assertThat(dao.getArticleWithComments(1)
            .get().books().stream().map(Book::id).collect(Collectors.toList())).containsExactly(1, 2);
        assertThat(dao.getArticleWithComments(2)
            .get().books().stream().map(Book::id).collect(Collectors.toList())).containsExactly(3, 4);
    }

    private interface PrivateBookDao extends SqlObject {
        @SqlQuery("select id, name from bookshelfs order by id")
        @RegisterImmutableMapper(Bookshelf.class)
        List<Bookshelf> listBookshelfs();

        @RegisterImmutableMapper(value = Bookshelf.class, prefix = "bookshelf")
        @RegisterImmutableMapper(value = Book.class, prefix = "book")
        default Optional<Bookshelf> getArticleWithComments(long id) {
            return getHandle().select(
                "select " +
                    "  bookshelfs.id     bookshelf_id, " +
                    "  bookshelfs.name   bookshelf_name, " +
                    "  books.id          book_id, " +
                    "  books.name        book_name " +
                    "from bookshelfs " +
                    "left join books " +
                    "  on bookshelfs.id = books.bookshelf_id " +
                    "where bookshelfs.id = ? " +
                    "order by books.id",
                id)
                .reduceRows(Optional.<Bookshelf>empty(),
                    (acc, rv) -> {
                        final Bookshelf a = acc.orElseGet(() -> rv.getRow(Bookshelf.class));

                        final List<Book> books = new LinkedList<>();
                        books.addAll(a.books());
                        if (rv.getColumn("bookshelf_id", Long.class) != null) {
                            books.add(rv.getRow(Book.class));
                        }

                        return Optional.of(new Bookshelf() {
                            @Override
                            public int id() {
                                return a.id();
                            }

                            @Override
                            public String name() {
                                return a.name();
                            }

                            @Override
                            public List<Book> books() {
                                return books;
                            }
                        });
                    });
        }
    }
}
