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
package org.jdbi.v3.core.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestJoinRowReducer {

    private static final String BOOK_COLUMNS = "b.id, b.title, b.isbn";
    private static final String AUTHOR_COLUMNS = "a.id author_id, a.name author_name";
    private static final String EDITOR_COLUMNS = "e.id editor_id, e.name editor_name";
    private static final String CATEGORY_COLUMNS = "c.id category_id, c.name category_name, c.description category_description";
    private static final String CHAPTER_COLUMNS = "ch.id chapter_id, ch.title chapter_title";
    private static final String TAG_COLUMNS = "t.id tag_id, t.name tag_name";

    private static final JoinRowReducer<Tag> TAGS = JoinRowReducer.of(Tag.class).mappedBy(ConstructorMapper::of);

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        handle.execute("CREATE TABLE persons (id INT PRIMARY KEY, name VARCHAR)");
        handle.execute("CREATE TABLE categories (id INT PRIMARY KEY, name VARCHAR, description VARCHAR, parent_id INT)");
        handle.execute("CREATE TABLE books (id INT PRIMARY KEY, title VARCHAR, isbn VARCHAR, author_id INT, editor_id INT, category_id INT)");
        handle.execute("CREATE TABLE chapters (id INT PRIMARY KEY, book_id INT, title VARCHAR)");
        handle.execute("CREATE TABLE tags (id INT PRIMARY KEY, name VARCHAR)");
        handle.execute("CREATE TABLE book_tags (book_id INT, tag_id INT)");
        handle.execute("CREATE TABLE category_tags (category_id INT, tag_id INT)");
        handle.execute("CREATE TABLE awards (id INT PRIMARY KEY, person_id INT, name VARCHAR)");

        handle.execute("INSERT INTO persons VALUES (1, 'Austen'), (2, 'Tolstoy'), (3, 'Unpublished'), (4, 'Ed')");
        handle.execute("INSERT INTO categories VALUES (1, 'Fiction', 'Made up', NULL), (2, 'Romance', 'Love', 1), (3, 'Regency', 'Empire', 2)");
        handle.execute("INSERT INTO books VALUES (1, 'Emma', '111', 1, 4, 2), (2, 'Persuasion', '222', 1, 2, 2),"
            + " (3, 'War and Peace', '333', 2, 4, 1), (4, 'Anonymous', '444', NULL, NULL, NULL)");
        handle.execute("INSERT INTO chapters VALUES (1, 1, 'Emma I'), (2, 1, 'Emma II'), (3, 3, 'W&P I')");
        handle.execute("INSERT INTO tags VALUES (1, 'classic'), (2, 'english'), (3, 'russian')");
        handle.execute("INSERT INTO book_tags VALUES (1, 1), (1, 2), (3, 1), (3, 3)");
        handle.execute("INSERT INTO category_tags VALUES (1, 1), (2, 2)");
        handle.execute("INSERT INTO awards VALUES (1, 1, 'Regency Prize'), (2, 1, 'Bath Medal'), (3, 2, 'Order of St. Anna')");
    }

    private static JoinRowReducer<Book> books() {
        return JoinRowReducer.of(Book.class).mappedBy(BeanMapper::of);
    }

    @Test
    public void oneToManyWithOuterJoin() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b LEFT JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(books().many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "Persuasion", "War and Peace", "Anonymous");
        assertThat(books.get(0).getChapters()).extracting(Chapter::getTitle).containsExactly("Emma I", "Emma II");
        assertThat(books.get(1).getChapters()).isEmpty();
        assertThat(books.get(2).getChapters()).extracting(Chapter::getTitle).containsExactly("W&P I");
        assertThat(books.get(3).getChapters()).isEmpty();
    }

    @Test
    public void manyToOneSharesReferencesAcrossRoots() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + CATEGORY_COLUMNS
                + " FROM books b LEFT JOIN persons a ON a.id = b.author_id LEFT JOIN categories c ON c.id = b.category_id ORDER BY b.id")
            .reduceRows(books()
                .one("author", Person.class, Book::setAuthor)
                .one("category", Category.class, Book::setCategory))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "Persuasion", "War and Peace", "Anonymous");
        assertThat(books.get(0).getAuthor()).isNotNull().isSameAs(books.get(1).getAuthor());
        assertThat(books.get(0).getAuthor().getName()).isEqualTo("Austen");
        assertThat(books.get(0).getCategory()).isSameAs(books.get(1).getCategory());
        assertThat(books.get(2).getAuthor().getName()).isEqualTo("Tolstoy");
        assertThat(books.get(2).getCategory().getName()).isEqualTo("Fiction");
        assertThat(books.get(2).getCategory().getDescription()).isEqualTo("Made up");
        assertThat(books.get(3).getAuthor()).isNull();
        assertThat(books.get(3).getCategory()).isNull();
    }

    @Test
    public void sameTypeThroughTwoRelationsIsOneInstance() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + EDITOR_COLUMNS
                + ", aw.id author_award_id, aw.name author_award_name, ew.id editor_award_id, ew.name editor_award_name"
                + " FROM books b"
                + " LEFT JOIN persons a ON a.id = b.author_id"
                + " LEFT JOIN persons e ON e.id = b.editor_id"
                + " LEFT JOIN awards aw ON aw.person_id = a.id"
                + " LEFT JOIN awards ew ON ew.person_id = e.id"
                + " ORDER BY b.id, aw.id, ew.id")
            .reduceRows(books()
                .one("author", JoinRowReducer.of(Person.class).many("award", Award.class, Person::addAward), Book::setAuthor)
                .one("editor", Person.class, Book::setEditor))
            .toList();

        Book emma = books.get(0);
        Book persuasion = books.get(1);
        Book warAndPeace = books.get(2);
        assertThat(persuasion.getEditor()).isSameAs(warAndPeace.getAuthor());
        assertThat(persuasion.getEditor().getName()).isEqualTo("Tolstoy");
        assertThat(persuasion.getEditor().getAwards()).extracting(Award::getName).containsExactly("Order of St. Anna");
        assertThat(emma.getAuthor()).isSameAs(persuasion.getAuthor());
        assertThat(emma.getAuthor().getAwards()).extracting(Award::getName).containsExactly("Regency Prize", "Bath Medal");
        assertThat(emma.getEditor()).isSameAs(warAndPeace.getEditor());
        assertThat(emma.getEditor().getAwards()).isEmpty();
        assertThat(books.get(3).getAuthor()).isNull();
        assertThat(books.get(3).getEditor()).isNull();
    }

    private static final String SELF_JOIN = "SELECT c.id, c.name, c.description, pc.id parent_id, pc.name parent_name, "
        + TAG_COLUMNS + ", pt.id parent_tag_id, pt.name parent_tag_name"
        + " FROM categories c"
        + " LEFT JOIN categories pc ON pc.id = c.parent_id"
        + " LEFT JOIN category_tags ct ON ct.category_id = c.id"
        + " LEFT JOIN tags t ON t.id = ct.tag_id"
        + " LEFT JOIN category_tags pct ON pct.category_id = pc.id"
        + " LEFT JOIN tags pt ON pt.id = pct.tag_id";

    private static JoinRowReducer<Category> categories() {
        return JoinRowReducer.of(Category.class).mappedBy(BeanMapper::of)
            .one("parent", Category.class, Category::setParent)
            .many("tag", TAGS, Category::addTag);
    }

    @Test
    public void selfJoinParentIsTheRootInstanceWhateverTheRowOrder() {
        List<Category> categories = handle.createQuery(SELF_JOIN + " ORDER BY c.id DESC")
            .reduceRows(categories())
            .toList();

        assertThat(categories).extracting(Category::getName).containsExactly("Regency", "Romance", "Fiction");
        Category romance = categories.get(1);
        Category fiction = categories.get(2);
        assertThat(categories.get(0).getParent()).isSameAs(romance);
        assertThat(romance.getParent()).isSameAs(fiction);
        assertThat(fiction.getDescription()).isEqualTo("Made up");
        assertThat(fiction.getParent()).isNull();
        assertThat(fiction.getTags()).extracting(Tag::name).containsExactly("classic");
        assertThat(romance.getTags()).extracting(Tag::name).containsExactly("english");
    }

    @Test
    public void onlyRootKeysBecomeRoots() {
        List<Category> categories = handle.createQuery(SELF_JOIN + " WHERE c.id = 2")
            .reduceRows(categories())
            .toList();

        assertThat(categories).extracting(Category::getName).containsExactly("Romance");
        Category fiction = categories.get(0).getParent();
        assertThat(fiction.getName()).isEqualTo("Fiction");
        assertThat(fiction.getDescription()).isNull();
        assertThat(fiction.getTags()).extracting(Tag::name).containsExactly("classic");
    }

    @Test
    public void recursionFollowsTheSelectedColumns() {
        List<Category> categories = handle.createQuery("SELECT c.id, c.name, pc.id parent_id, pc.name parent_name,"
                + " ppc.id parent_parent_id, ppc.name parent_parent_name"
                + " FROM categories c LEFT JOIN categories pc ON pc.id = c.parent_id LEFT JOIN categories ppc ON ppc.id = pc.parent_id"
                + " WHERE c.id = 3")
            .reduceRows(JoinRowReducer.of(Category.class).mappedBy(BeanMapper::of)
                .one("parent", Category.class, Category::setParent))
            .toList();

        Category regency = categories.get(0);
        assertThat(regency.getParent().getName()).isEqualTo("Romance");
        assertThat(regency.getParent().getParent().getName()).isEqualTo("Fiction");
        assertThat(regency.getParent().getParent().getParent()).isNull();
    }

    @Test
    public void keysOfDifferentIntegralTypesAreEqual() {
        List<Category> categories = handle.createQuery("SELECT c.id, c.name, CAST(pc.id AS BIGINT) parent_id, pc.name parent_name"
                + " FROM categories c LEFT JOIN categories pc ON pc.id = c.parent_id ORDER BY c.id")
            .reduceRows(JoinRowReducer.of(Category.class).mappedBy(BeanMapper::of)
                .one("parent", Category.class, Category::setParent))
            .toList();

        assertThat(categories.get(1).getParent()).isSameAs(categories.get(0));
    }

    @Test
    public void nullJoinedObjectWithRelationsIsNotLinked() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS
                + ", aw.id author_award_id, aw.name author_award_name"
                + " FROM books b JOIN persons a ON a.id = b.author_id LEFT JOIN awards aw ON aw.person_id = a.id ORDER BY b.id")
            .reduceRows(books()
                .one("author", JoinRowReducer.of(Person.class).mappedBy((type, prefix) -> (rs, ctx) -> null)
                    .many("award", Award.class, Person::addAward), Book::setAuthor))
            .toList();

        assertThat(books).hasSize(3).allSatisfy(book -> assertThat(book.getAuthor()).isNull());
    }

    @Test
    public void unprefixedCustomRootMapperNeedsAFactory() {
        handle.registerRowMapper(Book.class, (rs, ctx) -> new Book());

        assertThatThrownBy(() -> handle.createQuery("SELECT " + BOOK_COLUMNS + " FROM books b")
            .reduceRows(JoinRowReducer.of(Book.class))
            .toList())
            .isInstanceOf(NoSuchMapperException.class)
            .hasMessageContaining("mappedBy()");
    }

    @Test
    public void linkersSeeCompleteObjects() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS
                + ", aw.id author_award_id, aw.name author_award_name"
                + " FROM books b JOIN persons a ON a.id = b.author_id LEFT JOIN awards aw ON aw.person_id = a.id ORDER BY b.id, aw.id")
            .reduceRows(books()
                .one("author", JoinRowReducer.of(Person.class).many("award", Award.class, Person::addAward), (book, author) -> {
                    assertThat(author.getAwards()).isNotEmpty();
                    book.setAuthor(author);
                }))
            .toList();

        assertThat(books).hasSize(3).allSatisfy(book -> assertThat(book.getAuthor()).isNotNull());
    }

    @Test
    public void innerJoinOmitsRootsWithoutMatch() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(books().many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "War and Peace");
        assertThat(books.get(0).getChapters()).hasSize(2);
    }

    @Test
    public void twoToManyJoinsDoNotMultiplyChildren() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS + ", " + TAG_COLUMNS
                + " FROM books b"
                + " LEFT JOIN chapters ch ON ch.book_id = b.id"
                + " LEFT JOIN book_tags bt ON bt.book_id = b.id"
                + " LEFT JOIN tags t ON t.id = bt.tag_id"
                + " ORDER BY b.id, ch.id, t.id")
            .reduceRows(books()
                .many("chapter", Chapter.class, Book::addChapter)
                .many("tag", TAGS, Book::addTag))
            .toList();

        assertThat(books).hasSize(4);
        Book emma = books.get(0);
        assertThat(emma.getChapters()).extracting(Chapter::getTitle).containsExactly("Emma I", "Emma II");
        assertThat(emma.getTags()).extracting(Tag::name).containsExactly("classic", "english");
        Book warAndPeace = books.get(2);
        assertThat(warAndPeace.getTags()).extracting(Tag::name).containsExactly("classic", "russian");
        assertThat(warAndPeace.getTags().get(0)).isSameAs(emma.getTags().get(0));
        assertThat(books.get(3).getTags()).isEmpty();
    }

    @Test
    public void mixedRelations() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN persons a ON a.id = b.author_id LEFT JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(books()
                .one("author", Person.class, Book::setAuthor)
                .many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "Persuasion", "War and Peace");
        assertThat(books.get(0).getAuthor().getName()).isEqualTo("Austen");
        assertThat(books.get(0).getChapters()).hasSize(2);
        assertThat(books.get(1).getAuthor()).isSameAs(books.get(0).getAuthor());
        assertThat(books.get(1).getChapters()).isEmpty();
    }

    @Test
    public void registeredMappersByPrefix() {
        handle.registerRowMapper(BeanMapper.factory(Book.class));
        handle.registerRowMapper(BeanMapper.factory(Person.class, "author"));
        handle.registerRowMapper(BeanMapper.factory(Chapter.class, "chapter"));

        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN persons a ON a.id = b.author_id LEFT JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(JoinRowReducer.of(Book.class)
                .one("author", Person.class, Book::setAuthor)
                .many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "Persuasion", "War and Peace");
        assertThat(books.get(0).getAuthor().getName()).isEqualTo("Austen");
        assertThat(books.get(0).getChapters()).hasSize(2);
    }

    @Test
    public void registeredRootMapperIsTheUnprefixedOne() {
        handle.registerRowMapper(BeanMapper.factory(Category.class));
        handle.registerRowMapper(BeanMapper.factory(Category.class, "parent"));

        List<Category> categories = handle.createQuery("SELECT c.id, c.name, pc.id parent_id, pc.name parent_name"
                + " FROM categories c LEFT JOIN categories pc ON pc.id = c.parent_id ORDER BY c.id")
            .reduceRows(JoinRowReducer.of(Category.class).one("parent", Category.class, Category::setParent))
            .toList();

        assertThat(categories).extracting(Category::getName).containsExactly("Fiction", "Romance", "Regency");
        assertThat(categories.get(1).getParent()).isSameAs(categories.get(0));
        assertThat(categories.get(2).getParent()).isSameAs(categories.get(1));
    }

    @Test
    public void customMapperThroughFactory() {
        RowMapper<Book> upperCaseTitles = (rs, ctx) -> {
            Book book = new Book();
            book.setId(rs.getInt("id"));
            book.setTitle(rs.getString("title").toUpperCase(Locale.ROOT));
            return book;
        };

        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(JoinRowReducer.of(Book.class)
                .mappedBy((type, prefix) -> type == Book.class ? upperCaseTitles : BeanMapper.of(type, prefix))
                .many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("EMMA", "WAR AND PEACE");
        assertThat(books.get(0).getChapters()).extracting(Chapter::getTitle).containsExactly("Emma I", "Emma II");
    }

    @Test
    public void customKeyColumn() {
        List<Book> books = handle.createQuery("SELECT b.isbn, b.title, " + CHAPTER_COLUMNS
                + " FROM books b LEFT JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(JoinRowReducer.of(Book.class, "isbn").mappedBy(BeanMapper::of)
                .many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).extracting(Book::getIsbn).containsExactly("111", "222", "333", "444");
        assertThat(books.get(0).getChapters()).hasSize(2);
    }

    @Test
    public void nullRootKeySkipsRow() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS
                + " FROM books b RIGHT JOIN persons a ON a.id = b.author_id ORDER BY a.id, b.id")
            .reduceRows(books().one("author", Person.class, Book::setAuthor))
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "Persuasion", "War and Peace");
    }

    @Test
    public void reducerIsReusable() {
        JoinRowReducer<Book> reducer = books().many("chapter", Chapter.class, Book::addChapter);
        String sql = "SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
            + " FROM books b LEFT JOIN chapters ch ON ch.book_id = b.id WHERE b.id = :id ORDER BY ch.id";

        List<Book> first = handle.createQuery(sql).bind("id", 1).reduceRows(reducer).toList();
        List<Book> second = handle.createQuery(sql).bind("id", 3).reduceRows(reducer).toList();

        assertThat(first).extracting(Book::getTitle).containsExactly("Emma");
        assertThat(first.get(0).getChapters()).hasSize(2);
        assertThat(second).extracting(Book::getTitle).containsExactly("War and Peace");
        assertThat(second.get(0).getChapters()).hasSize(1);
    }

    @Test
    public void linkingIsImmutable() {
        JoinRowReducer<Book> withAuthor = books().one("author", Person.class, Book::setAuthor);
        JoinRowReducer<Book> withAuthorAndChapters = withAuthor.many("chapter", Chapter.class, Book::addChapter);
        String withoutChapters = "SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS
            + " FROM books b JOIN persons a ON a.id = b.author_id WHERE b.id = 1";
        String withChapters = "SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + CHAPTER_COLUMNS
            + " FROM books b JOIN persons a ON a.id = b.author_id JOIN chapters ch ON ch.book_id = b.id WHERE b.id = 1";

        Book authorOnly = handle.createQuery(withoutChapters).reduceRows(withAuthor).findFirst().orElseThrow();
        Book both = handle.createQuery(withChapters).reduceRows(withAuthorAndChapters).findFirst().orElseThrow();

        assertThat(authorOnly.getChapters()).isEmpty();
        assertThat(both.getChapters()).hasSize(2);
    }

    @Test
    public void binaryKeysAreComparedByContent() {
        handle.execute("CREATE TABLE shelves (id BINARY(16) PRIMARY KEY, title VARCHAR)");
        handle.execute("CREATE TABLE shelf_chapters (id INT PRIMARY KEY, shelf_id BINARY(16), title VARCHAR)");
        handle.execute("INSERT INTO shelves VALUES (X'0102', 'Emma')");
        handle.execute("INSERT INTO shelf_chapters VALUES (1, X'0102', 'I'), (2, X'0102', 'II')");

        List<Shelf> shelves = handle.createQuery("SELECT s.id, s.title, " + CHAPTER_COLUMNS
                + " FROM shelves s JOIN shelf_chapters ch ON ch.shelf_id = s.id ORDER BY ch.id")
            .reduceRows(JoinRowReducer.of(Shelf.class).mappedBy(BeanMapper::of)
                .many("chapter", Chapter.class, Shelf::addChapter))
            .toList();

        assertThat(shelves).hasSize(1);
        assertThat(shelves.get(0).getChapters()).extracting(Chapter::getTitle).containsExactly("I", "II");
    }

    @Test
    public void subclassForUseRowReducer() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(new BookReducer())
            .toList();

        assertThat(books).extracting(Book::getTitle).containsExactly("Emma", "War and Peace");
        assertThat(books.get(0).getChapters()).hasSize(2);
    }

    @Test
    public void oneLinksOncePerRoot() {
        AtomicInteger authorLinks = new AtomicInteger();

        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN persons a ON a.id = b.author_id LEFT JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(books()
                .one("author", Person.class, (book, author) -> {
                    authorLinks.incrementAndGet();
                    book.setAuthor(author);
                })
                .many("chapter", Chapter.class, Book::addChapter))
            .toList();

        assertThat(books).hasSize(3);
        assertThat(books.get(0).getChapters()).hasSize(2);
        assertThat(authorLinks).hasValue(3);
    }

    @Test
    public void oneRejectsSecondKeyForSameRoot() {
        assertThatThrownBy(() -> handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + CHAPTER_COLUMNS
                + " FROM books b JOIN chapters ch ON ch.book_id = b.id ORDER BY b.id, ch.id")
            .reduceRows(books().one("chapter", Chapter.class, Book::addChapter))
            .toList())
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("chapter_id")
            .hasMessageContaining("many()");
    }

    @Test
    public void nullRootFails() {
        assertThatThrownBy(() -> handle.createQuery("SELECT " + BOOK_COLUMNS + " FROM books b")
            .reduceRows(JoinRowReducer.of(Book.class).mappedBy((type, prefix) -> (rs, ctx) -> null))
            .toList())
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("id = 1");
    }

    @Test
    public void nullJoinedObjectIsNotLinked() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS
                + " FROM books b JOIN persons a ON a.id = b.author_id ORDER BY b.id")
            .reduceRows(books()
                .one("author", JoinRowReducer.of(Person.class).mappedBy((type, prefix) -> (rs, ctx) -> null), Book::setAuthor))
            .toList();

        assertThat(books).hasSize(3).allSatisfy(book -> assertThat(book.getAuthor()).isNull());
    }

    @Test
    public void missingRootKeyColumnFails() {
        assertThatThrownBy(() -> handle.createQuery("SELECT b.title FROM books b")
            .reduceRows(books())
            .toList())
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("root key column id");
    }

    @Test
    public void missingRelationKeyColumnFails() {
        assertThatThrownBy(() -> handle.createQuery("SELECT " + BOOK_COLUMNS + ", a.id athor_id, a.name athor_name"
                + " FROM books b JOIN persons a ON a.id = b.author_id")
            .reduceRows(books().one("author", Person.class, Book::setAuthor))
            .toList())
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("author_id")
            .hasMessageContaining("\"author\"");
    }

    @Test
    public void relationSelectedOnOnePathIsEnough() {
        List<Book> books = handle.createQuery("SELECT " + BOOK_COLUMNS + ", " + AUTHOR_COLUMNS + ", " + EDITOR_COLUMNS
                + ", aw.id author_award_id, aw.name author_award_name"
                + " FROM books b"
                + " LEFT JOIN persons a ON a.id = b.author_id"
                + " LEFT JOIN persons e ON e.id = b.editor_id"
                + " LEFT JOIN awards aw ON aw.person_id = a.id"
                + " ORDER BY b.id, aw.id")
            .reduceRows(books()
                .one("author", JoinRowReducer.of(Person.class).many("award", Award.class, Person::addAward), Book::setAuthor)
                .one("editor", Person.class, Book::setEditor))
            .toList();

        assertThat(books.get(1).getEditor().getAwards()).extracting(Award::getName).containsExactly("Order of St. Anna");
        assertThat(books.get(0).getEditor().getAwards()).isEmpty();
    }

    @Test
    public void rejectsTwoReducersForOneType() {
        JoinRowReducer<Book> withAuthor = books().one("author", JoinRowReducer.of(Person.class), Book::setAuthor);

        assertThatThrownBy(() -> withAuthor.one("editor", JoinRowReducer.of(Person.class), Book::setEditor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(Person.class.getName());
    }

    @Test
    public void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> JoinRowReducer.of((Class<Book>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> JoinRowReducer.of(Book.class, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> books().mappedBy(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> books().one("author", Person.class, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> books().many(null, Chapter.class, Book::addChapter)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> books().many("", Chapter.class, Book::addChapter)).isInstanceOf(IllegalArgumentException.class);
    }

    public static class BookReducer extends JoinRowReducer<Book> {
        public BookReducer() {
            super(books().many("chapter", Chapter.class, Book::addChapter));
        }
    }

    public static class Book {
        private int id;
        private String title;
        private String isbn;
        private Person author;
        private Person editor;
        private Category category;
        private final List<Chapter> chapters = new ArrayList<>();
        private final List<Tag> tags = new ArrayList<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public Person getAuthor() {
            return author;
        }

        public void setAuthor(Person author) {
            this.author = author;
        }

        public Person getEditor() {
            return editor;
        }

        public void setEditor(Person editor) {
            this.editor = editor;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public List<Chapter> getChapters() {
            return chapters;
        }

        public void addChapter(Chapter chapter) {
            chapters.add(chapter);
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void addTag(Tag tag) {
            tags.add(tag);
        }
    }

    public static class Person {
        private int id;
        private String name;
        private final List<Award> awards = new ArrayList<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Award> getAwards() {
            return awards;
        }

        public void addAward(Award award) {
            awards.add(award);
        }
    }

    public static class Award {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Category {
        private int id;
        private String name;
        private String description;
        private Category parent;
        private final List<Tag> tags = new ArrayList<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Category getParent() {
            return parent;
        }

        public void setParent(Category parent) {
            this.parent = parent;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public void addTag(Tag tag) {
            tags.add(tag);
        }
    }

    public static class Chapter {
        private int id;
        private String title;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public record Tag(int id, String name) {}

    public static class Shelf {
        private byte[] id;
        private String title;
        private final List<Chapter> chapters = new ArrayList<>();

        public byte[] getId() {
            return id;
        }

        public void setId(byte[] id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Chapter> getChapters() {
            return chapters;
        }

        public void addChapter(Chapter chapter) {
            chapters.add(chapter);
        }
    }
}
