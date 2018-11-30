![Jdbi Logo](docs/src/adoc/images/logo.svg)

[![Build Status](https://travis-ci.org/jdbi/jdbi.svg?branch=master)](https://travis-ci.org/jdbi/jdbi)

The Jdbi library provides convenient, idiomatic access to relational databases
in Java.

Jdbi is built on top of JDBC. If your database has a JDBC driver, you can use
Jdbi with it.

* [Developer Guide and API Docs](https://jdbi.github.io/)
* [Mailing List](http://groups.google.com/group/jdbi)

## Prerequisites

Jdbi 3 requires Java 8 or better.

We run CI tests against all currently supported versions, and recommend Java 11.

## Builds

Jdbi is built with Maven:

```bash
$ mvn clean install
```

The tests use real Postgres and H2 databases.

You do not need to install anything--the tests will spin up
temporary database servers as needed.

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
