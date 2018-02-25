# Reactive Relational Database Connectivity (R2DBC)
This project is an exploration of what a Java API for relational database access with [Reactive Streams][rs] might look like.  It uses [Project Reactor][pr] and only contains an implementation for [PostgreSQL][pg].  It uses [Jdbi][jd] as an inspiration.

**THIS IS ONLY AN EXPERIEMENT AND NOT SUPPORTED SOFTWARE**

[jd]: http://jdbi.org
[pg]: https://www.postgresql.org
[pr]: https://projectreactor.io
[rs]: http://www.reactive-streams.org


## Examples
Full fledged examples can be found in the [`r2dbc-examples`][re] project.  A quick example of configuration and execution would look like:

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
            .execute(result -> result.map((row, rowMetadata) -> row.get("value", Integer.class)))))

    .subscribe(System.out::println);
```

**THE PROJECT SYNTAX IS HIGHLY VOLATILE AND THIS EXAMPLE MAY BE OUT OF DATE**

[re]: r2dbc-examples/src/test/java/com/nebhale/r2dbc/examples/CoreExamples.java


## Maven
Both milestone and snapshot artifacts (library, source, and javadoc) can be found in Maven repositories.  The database implementation artifact (`r2dbc-postgresql`) is the only artifact that needs to be directly included.

```xml
<dependency>
  <groupId>com.nebhale.r2dbc</groupId>
  <artifactId>r2dbc-postgresql</artifactId>
  <version>1.0.0.M1</version>
</dependency>
```

Artifacts can bound found at the following repositories.

### Repositories
```xml
<repository>
    <id>nebhale-snapshots</id>
    <url>https://raw.githubusercontent.com/nebhale/r2dbc/maven/snapshot</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

```xml
<repository>
    <id>nebhale-milestones</id>
    <url>https://raw.githubusercontent.com/nebhale/r2dbc/maven/milestone</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
```

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0
