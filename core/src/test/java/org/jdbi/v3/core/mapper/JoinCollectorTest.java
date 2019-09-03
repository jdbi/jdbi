package org.jdbi.v3.core.mapper;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.immutables.value.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JoinCollectorTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    Handle h;

    List<Author> authors = new ArrayList<>();
    List<Book> books = new ArrayList<>();

    Author a, b, c, d;
    Book pb, cm, cd, bt;

    @Before
    public void setup() {
        h = db.getSharedHandle();
        h.getConfig(JdbiImmutables.class)
            .registerImmutable(Book.class, Author.class);

        h.execute("create table book (bookId int not null, name varchar not null, authorId int)");
        h.execute("create table author (authorId int not null, name varchar not null)");

        a = addAuthor(1, "Alice");
        b = addAuthor(2, "Bob");
        c = addAuthor(3, "Carol");
        d = addAuthor(4, "Dave");

        pb = addBook("Alice", 1, "Pretty Birds");
        cm = addBook("Alice", 2, "Curious Mammals");
        cd = addBook("Bob", 3, "Cooking for Dummies");
        bt = addBook("Carol", 4, "Big Trucks");
    }

    private Author addAuthor(int authorId, String authorName) {
        Author au = author(authorId, authorName);
        authors.add(au);
        h.createUpdate("insert into author (authorId, name) values (:authorId, :name)")
                .bindPojo(au)
                .execute();
        return au;
    }

    private Book addBook(String authorName, int bookId, String bookName) {
        Book bk = book(bookId, bookName,
                authors.stream()
                    .filter(bookAuthor -> bookAuthor.name().equals(authorName))
                    .findAny()
                    .orElseThrow(AssertionError::new));
        books.add(bk);
        h.createUpdate("insert into book (bookId, name, authorId) values(:bookId, :name, :authorId)")
                .bindPojo(bk)
                .execute();
        return bk;
    }

    @Test
    public void naturalJoin() {
        assertThat(h.createQuery("select * from book natural join author")
                .collectInto(Library.class))
            .extracting(Library::books)
            .isEqualTo(ImmutableMultimap.builder()
                    .put(a, pb)
                    .put(a, cm)
                    .put(b, cd)
                    .put(c, bt)
                    .build());
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
