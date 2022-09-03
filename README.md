![Jdbi Logo](docs/src/adoc/images/logo.svg)

[![CD from master pushes](https://github.com/jdbi/jdbi/actions/workflows/cd.yml/badge.svg)](https://github.com/jdbi/jdbi/actions/workflows/cd.yml) |
[![CI Build with tests](https://github.com/jdbi/jdbi/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbi/jdbi/actions/workflows/ci.yml) | [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/jdbi/jdbi.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/jdbi/jdbi/context:java)

The Jdbi library provides convenient, idiomatic access to relational databases
in Java.

Jdbi is built on top of JDBC. If your database has a JDBC driver, you can use
Jdbi with it.

* [Developer Guide](https://jdbi.github.io/)
* [Javadoc](https://jdbi.org/apidocs/)
* [Mailing List](http://groups.google.com/group/jdbi)

Also check out the code examples in the [Examples](https://github.com/jdbi/jdbi/blob/master/examples/README.md) module.

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

## Building

Jdbi is "batteries included" and uses the [Apache Maven Wrapper](https://maven.apache.org/wrapper/). If an external Maven installation is used, Apache Maven 3.8.6 or later is recommended.

Jdbi requires a modern JDK (11+) to build and enforces JDK 17+ for releases.

All build tasks are organized as `make` targets. The Makefile in the root directory shows which commands are run.

Build the code an install it into the local repository:

```bash
$ make install
```

Running `make` or `make help` displays all available build targets with a short explanation. Some of the goals will require project membership privileges.

To add command line parameters to the maven executions from the Makefile, set the `JDBI_MAVEN_OPTS` variable:

``` bash
% JDBI_MAVEN_OPTS="-B -fae" make install
```

## Testing

Running `make tests` runs all unit and integration tests.

Some tests use Postgres and H2 databases (the tests will spin up temporary database servers as needed). Most modern OS (Windows, MacOS, Linux) and host architecture (x86_64, aarch64) should work.

### Docker requirements for tests

For a full release build, docker or a docker compatible environment
must be available. A small number of tests (those supporting the OTJ
postgres plugin) use testcontainers which in turn requires docker.

`make install-nodocker` skips the tests when building and installing Jdbi locally. `make tests-nodocker` skips the tests when only running tests.

Supported configurations are

* Docker Desktop on MacOS
* docker-ce on Linux
* podman 3 or better on Linux.

For podman, the podman socket must be activated (see
https://stackoverflow.com/questions/71549856/testcontainers-with-podman-in-java-tests)
for details. SELinux sometimes interferes with testcontainers if
SELinux is active; make sure that there is an exception configured.

## Contributing

Please read
[CONTRIBUTING.md](https://github.com/jdbi/jdbi/blob/master/CONTRIBUTING.md)
for instructions to set up your development environment to build Jdbi.

## Versioning

Jdbi uses [SemVer](http://semver.org/) for versioning.

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
