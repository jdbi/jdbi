![Jdbi Logo](docs/src/adoc/images/logo.svg)

[![CI Build with tests](https://github.com/jdbi/jdbi/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbi/jdbi/actions/workflows/ci.yml) | [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/jdbi/jdbi.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/jdbi/jdbi/context:java)

The Jdbi library provides convenient, idiomatic access to relational databases
in Java.

Jdbi is built on top of JDBC. If your database has a JDBC driver, you can use
Jdbi with it.

* [Developer Guide](https://jdbi.github.io/)
* [Javadoc](https://jdbi.org/apidocs/)
* [Mailing List](http://groups.google.com/group/jdbi)

## Prerequisites

Jdbi 3 requires Java 8 or better to run. Jdbi 3 requires Java 11 or better to compile.

We run CI tests against Java 11 and 17 and still support Java 8 for testing on a best-effort basis.

### Java 8 compatibility

Java 8 is considered deprecated. While Jdbi does not (yet) have a specific date to drop support,
please chart your path forward to a supported JDK! We recommend running the latest LTS JDK.

Jdbi 3 is compiled to Java 8 byte code and is considered stable on Java 8.

However, we now require Java 11 or better to compile as the tool chain no longer runs on Java 8.

We run CI tests on Java 8 on a best effort basis as some of the tests require Java 11+ only dependencies.

NOTE: to run on Java 8, you may need to manage the `caffeine` dependency back to the
latest 2.x release. 3.x is necessary for newer JDKs but does not run on 8.

## Builds

Jdbi is built with Apache Maven, requiring version 3.6.0 or newer.

```bash
$ ./mvnw clean install
```

The unit tests use Postgres and H2 databases (the tests will spin up temporary database servers as needed).

## Contributing

Please read
[CONTRIBUTING.md](https://github.com/jdbi/jdbi/blob/master/CONTRIBUTING.md)
for instructions to set up your development environment to build Jdbi.

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## License

This project is licensed under the
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Project Members

* **Brian McCallister (@brianm)** - Project Founder
* **Steven Schlansker (@stevenschlansker)**
* **Henning Schmiedehausen (@hgschmie)**
* **Matthew Hall (@qualidafial)**
* **Artem Prigoda (@arteam)**
* **Marnick L'Eau (@TheRealMarnes)**

## Special Thanks

* **Alex Harin ([@aharin](https://github.com/aharin))** - Kotlin plugins.
* **Ali Shakiba ([@shakiba](https://github.com/shakiba))** - JPA plugin
* **[@alwins0n](https://github.com/alwins0n)** - Vavr plugin.
* **Fred Deschenes ([@FredDeschenes](https://github.com/FredDeschenes))** -
  Kotlin unchecked extensions for `Jdbi` functions. `@BindFields`,
  `@BindMethods` annotations.
