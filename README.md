![Jdbi Logo](docs/src/adoc/images/logo.svg)

[![CD from master pushes](https://github.com/jdbi/jdbi/actions/workflows/cd.yml/badge.svg)](https://github.com/jdbi/jdbi/actions/workflows/cd.yml) |
[![CI Build with tests](https://github.com/jdbi/jdbi/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbi/jdbi/actions/workflows/ci.yml) | [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jdbi_jdbi&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=jdbi_jdbi) | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jdbi_jdbi&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=jdbi_jdbi) | [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jdbi_jdbi&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=jdbi_jdbi)

The Jdbi library provides convenient, idiomatic access to relational databases in Java and other JVM technologies such as Kotlin, Clojure or Scala.

Jdbi is built on top of JDBC. If your database has a JDBC driver, you can use Jdbi with it.


* [Developer Guide](https://jdbi.org/)
* [Javadoc](https://jdbi.org/apidocs/)
* [User forums](https://github.com/jdbi/jdbi/discussions)
* [Mailing List](http://groups.google.com/group/jdbi)
* [Stack Overflow](https://stackoverflow.com/questions/tagged/jdbi)

Also check out the code examples in the [Examples](https://github.com/jdbi/jdbi/tree/master/examples) module.


## Prerequisites

Jdbi requires Java 11 or better to run. Jdbi requires Java 11 or better to compile.

We run CI tests against Java 11, 17 and 21.


### Compatibility with older Java versions

Java 8, 9 and 10 are supported by  any Jdbi version before **3.40.0**.

## Building

Jdbi requires JDK 11+ to build and enforces JDK 17+ for releases.

Jdbi is "batteries included" and uses the [Apache Maven Wrapper](https://maven.apache.org/wrapper/). If an external Maven installation is used, Apache Maven 3.9 or later is required. Using the `make` targets requires GNU make.

All build tasks are organized as `make` targets.

Build the code an install it into the local repository:

```bash
$ make install
```

Running `make` or `make help` displays all available build targets with a short explanation. Some of the goals will require project membership privileges.  The [CONTRIBUTING.md](https://github.com/jdbi/jdbi/blob/master/CONTRIBUTING.md) document contains a list of all supported targets.

To add command line parameters to the maven executions from the Makefile, set the `MAVEN_CONFIG` variable:

``` bash
% MAVEN_CONFIG="-B -fae" make install
```


## Testing

Running `make tests` runs all unit and integration tests.

Some tests use Postgres and H2 databases (the tests will spin up temporary database servers as needed). Most modern OS (Windows, MacOS, Linux) and host architecture (x86_64, aarch64) should work.


### Docker requirements

For a full release build, docker or a docker compatible environment
must be available. A small number of tests use testcontainers which in
turn requires docker.

`make install-nodocker` skips the tests when building and installing Jdbi locally. `make tests-nodocker` skips the tests when only running tests.

Supported configurations are

* Docker Desktop on MacOS
* docker-ce on Linux
* podman 3 or better on Linux and MacOS

Other docker installations such as [Colima](https://github.com/abiosoft/colima) may work but are untested and unsupported.

For podman on Linux, the podman socket must be activated (see
https://stackoverflow.com/questions/71549856/testcontainers-with-podman-in-java-tests)
for details. SELinux sometimes interferes with testcontainers if
SELinux is active; make sure that there is an exception configured.

For podman on MacOS, it is necessary to set the `DOCKER_HOST` environment variable correctly.


## Contributing

Please read
[CONTRIBUTING.md](https://github.com/jdbi/jdbi/blob/master/CONTRIBUTING.md)
for instructions to set up your development environment to build Jdbi.


## Versioning

Jdbi uses [SemVer](http://semver.org/) to version its public API.


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
