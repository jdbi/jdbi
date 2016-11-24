JPA annotation extension for jDBI
===============

Extension for [JDBI](https://github.com/jdbi/jdbi/) to use JPA annotations for mapping/binding instead of JavaBeans
conventions.

Special thanks to [@shakiba](https://github.com/shakiba) for donating the original code for this artifact.

### Usage

#### Annotate

Annotate your entity with JPA annotations:

```java
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Something {
    @Column private int id;
    @Column private String name;
}
```

#### Map

Use `JpaMapper` to create `RowMapper`:

```java
RowMapper<Something> mapper = JpaMapper.get(Something.class);
```

Or register `JpaMapperFactory` as a `RowMapperFactory`:

```java
@RegisterRowMapperFactory(JpaMapperFactory.class)
public interface SomethingDAO {

    @SqlQuery("select * from Something where id = :id")
    Something get(@Bind("id") long id);

}
```
#### Bind

Use `@BindJpa` instead of `@BindBean` to bind annotated classes.

```java
public interface SomethingDAO {

    @SqlUpdate("update something set name = :s.name where id = :s.id")
    void update(@BindJpa("s") Something something);

}
```

### Maven

```xml
<dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi-jpa</artifactId>
    <version>${jdbi.version}</version>
</dependency>
```
