JPA annotation extension for Jdbi
===============

Extension for [Jdbi](https://github.com/jdbi/jdbi/) to use JPA annotations for mapping/binding instead of JavaBeans
conventions.

Special thanks to [@shakiba](https://github.com/shakiba) for donating the original code for this artifact.

### Usage

#### Maven

```xml
<dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-jpa</artifactId>
    <version>${jdbi.version}</version>
</dependency>
```
#### Configure Jdbi to use the JPA plugin:

```java
jdbi.installPlugin(new JpaPlugin()));
```

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

Note that `@Entity` and `@Column` are the only annotations supported by this plugin.

#### Map

Your entity type will be automatically mapped:

```java
Something result = handle.select("select * from Something where id = ?", id)
    .mapTo(Something.class)
    .one();
```

```java
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
