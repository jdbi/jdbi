package org.jdbi.v3.core.mapper;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;
import org.immutables.value.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JoinMapperTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    Handle h;

    List<Author> authors = new ArrayList<>();
    List<Book> books = new ArrayList<>();

    @Before
    public void setup() {
        h = db.getSharedHandle();
        h.getConfig(JdbiImmutables.class)
            .registerImmutable(Book.class, Author.class);

        h.execute("create table books (bookId int not null, name varchar not null, authorId int)");
        h.execute("create table author (authorId int not null, name varchar not null)");

        addAuthors(
                "Alice",
                "Bob",
                "Carol",
                "Dave"
        );

        Book pb = addBook("Alice", 1, "Pretty Birds"),
             cm = addBook("Alice", 2, "Curious Mammals"),
             cd = addBook("Bob", 3, "Cooking for Dummies"),
             bt = addBook("Carol", 4, "Big Trucks");
    }

    private void addAuthors(String... authorNames) {
        for (int i = 0; i < authorNames.length; i++) {
            Author a = author(i, authorNames[i]);
            authors.add(a);
            h.createUpdate("insert into author (authorId, name) values (:authorId, :name)")
                .bindPojo(a)
                .execute();
        }
    }

    private Book addBook(String authorName, int bookId, String bookName) {
        Book b = book(bookId, bookName, authors.stream()
                    .filter(a -> a.name().equals(authorName))
                    .findAny()
                    .orElseThrow(AssertionError::new));
        books.add(b);
        h.createUpdate("insert into book (bookId, name, authorId) values(:bookId, :name, :authorId)")
                .bindPojo(b)
                .execute();
        return b;
    }

    @Test
    public void naturalJoin() {
        assertThat(h.createQuery("select * from book natural join author")
                .mapTo(Library.class)
                .execute());
    }

    @Value.Immutable
    public interface Library {
        Multimap<Author, Book> books();
    }

    @Value.Immutable
    public interface Book {
        int bookId();
        String name();
        int authorId();
    }

    @Value.Immutable
    public interface Author {
        int authorId();
        String name();
    }

    static Author author(int id, String name) {
        return ImmutableAuthor.builder().authorId(id).name(name).build();
    }

    static Book book(int id, String name, Author author) {
        return ImmutableBook.builder().bookId(id).name(name).authorId(author.authorId()).build();
    }
}
