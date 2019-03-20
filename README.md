# Reactive Relational Database Connectivity Client (R2DBC)
This project is an exploration of what a Java API for relational database access with [Reactive Streams][rs] might look like.  It uses [Project Reactor][pr].  It uses [Jdbi][jd] as an inspiration.

[jd]: http://jdbi.org
[pr]: https://projectreactor.io
[rs]: https://www.reactive-streams.org

## Examples
A quick example of configuration and execution would look like:

```java
PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
    .host("<host>")
    .database("<database>")
    .username("<username>")
    .password("<password>")
    .build();

R2dbc r2dbc = new R2dbc(new PostgresqlConnectionFactory(configuration));

r2dbc.inTransaction(handle ->
    handle.execute("INSERT INTO test VALUES ($1)", 100))

    .thenMany(r2dbc.inTransaction(handle ->
        handle.select("SELECT value FROM test")
            .mapResult(result -> result.map((row, rowMetadata) -> row.get("value", Integer.class)))))

    .subscribe(System.out::println);
```

## Maven
Both milestone and snapshot artifacts (library, source, and javadoc) can be found in Maven repositories.

```xml
<dependency>
  <groupId>io.r2dbc</groupId>
  <artifactId>r2dbc-client</artifactId>
  <version>1.0.0.M6</version>
</dependency>
```

Artifacts can bound found at the following repositories.

### Repositories
```xml
<repository>
    <id>spring-snapshots</id>
    <name>Spring Snapshots</name>
    <url>https://repo.spring.io/snapshot</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

```xml
<repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
```

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0
