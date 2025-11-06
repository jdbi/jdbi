![Jdbi Logo](docs/src/adoc/images/logo.svg)

Hi! Welcome to Jdbi.

We're glad you're thinking about contributing to the project.

## Getting in touch

We use [GitHub](https://github.com/jdbi/jdbi) as our central development hub. [Issues](https://github.com/jdbi/jdbi/issues), [Pull Requests](https://github.com/jdbi/jdbi/pulls) and [Discussions](https://github.com/jdbi/jdbi/discussions) all happen here.

We also have a [Mailing list](http://groups.google.com/group/jdbi) and occasionally monitor [Stack Overflow](https://stackoverflow.com/questions/tagged/jdbi) but using GitHub is the preferred way of communication.

## Contributing to the project

If you find a bug that you can reproduce, please file an issue with the project. Jdbi is a project of volunteers and we give no time lines or guarantees when a bug will be addressed.

Providing a bug and a fix is very welcome; send us a pull request. Having a test that demonstrates the bug and the fix will expedite getting your change into the code base.

The Jdbi code base follows some basic coding rules. Some are documented below, others are currently tribal knowledge (we apologize for that) but we will document them as we go along. We may provide feedback on PRs asking you to make changes to your code even if it was not obvious when you wrote the code and there were no documented rules. This is unfortunate and we apologize for that in advance.

We value backwards compatibility for our API. Large PRs that affect the public API will receive a lot of scrutiny.

If you plan to make a larger change or contribution, please discuss this first in the [discussion forums](https://github.com/jdbi/jdbi/discussions) using the [Ideas](https://github.com/jdbi/jdbi/discussions/categories/ideas) category.

### Jdbi coding guidelines

We strive for a healthy balance between subjective perfection and practical considerations, but we are firmly against doing a quick and incomplete job that will require more follow-up work later.

* Use the latest LTS (Java 25 right now) or newer.
* whenever possible, use "speaking" names. `handle`, not `h`.
* make minimal changes to the code. If you can hide an internal class, do so. If you can make an internal class final, do so.
* Jdbi is a library and any dependency that we use, we also force upon our users. Minimize the footprint of external dependencies; when in doubt we are more comfortable with copying a single class under Apache license into the code base with proper attribution over pulling in another dependency.
* we prefer stateless, immutable objects over anything else.
* We like both constructors and factory methods/builders, but require that they are used appropriately. Constructors are great for dumb classes, factories are better in case any defensive logic is involved.
* Some fundamental classes (`Jdbi`, anything config related) must be thread-safe. Others (such as `Handle` and the statement classes) don't need to. Clearly attribute if a class must be single-threaded (can only be used by one thread), is thread-safe (can be used by multiple threads at the same time) or in between (e.g. can be used by multiple threads but must be one thread at a time). If a class is not safe for multiple threads, clearly state so.

*Please run `make install` locally before opening a PR. We run lots of code and style checkers on the full build and failing those on a PR means we will not look at it before you fixed those. Your local build run from the command line should pass.*

### Backward compatibility

Jdbi places serious emphasis on not breaking compatibility. Remember these simple rules and think twice before making any classes or class members `public`!

1) what comes into the API, stays in the API (or: no is temporary, but yes is forever);
2) if a piece of API must be discouraged after public release, mark it `@Deprecated` and keep it functionally intact;
3) breaking cleanup work can be done when Jdbi is gearing up for a major version number increment (see [SemVer](https://semver.org/)). We also reserve the right to make backwards compatible changes when we change the minimally supported JDK version.
4) bug fixes that **absolutely require** an API change are the only exception.

If you must make some internal code `public` to access it from other packages, put the class in a package named `internal`. Packages named so are not considered API.

### Forward compatibility

Completely new APIs should be marked with [@Alpha](https://jdbi.org/apidocs/org/jdbi/v3/meta/Alpha.html) or [@Beta](https://jdbi.org/apidocs/org/jdbi/v3/meta/Beta.html). This lets users know not to rely too much on your changes yet, as the public release might reveal that more work needs to be done.

### Functionality

Jdbi should be useful for as many projects as possible with as little work as possible, within reason. It should be useful out of the box with sane defaults, but always configurable to the extent users are likely to need.

### Testing

* we use JUnit 5 for all our tests and assertj as assertion framework.
* Our tests describe and verify `Jdbi` behavior, changes to their behavior needs to be discussed and we will reject unnecessary test changes.
* Spin up a database using either the core testing framework (`H2DatabaseExtension` and `PgDatabaseExtension`) if you contribute to the core repository or the `testing` extensions (`JdbiExtension`) for all other modules.
* Focus on functionality and clarity for tests first, worry about performance afterwards. (A full build executing ~1,650 tests against hundreds of started and stopped Postgres and H2 instances takes about 200 seconds using JDK 19, `mvnd` on a 2021 Macbook Pro. And we run the tests on the CI anyway).
* do not use mocks or any mocking frameworks in the tests. We use Mockito in a few places and every single one is a problem and a pain to maintain.

The use of mocks and mocking framework is generally discouraged. There are a number of existing tests that use Mockito and we do not want to add more.

## Development Setup

Most modern IDEs configure themselves correctly by importing the Jdbi repository as an Apache Maven project. If necessary, install support for Apache Maven first.

Jdbi is mostly Java but has a few Kotlin modules. Therefore the IDE must also support Kotlin (either natively or by executing the maven build).

### Code Style / Formatting Rules

The project uses a set of code style and formatting rules. These are enforced by Checkstyle and PMD as part of the build cycle.

We do not review or merge PRs that do not pass our pre-merge checks. Run the build locally using `make install`.

### Make driven build

There is a Makefile at the root of the project to drive the various builds. Run `make` or `make help` to display all available goals. Some goals are privileged (you need to be a member of the Jdbi development team). Generally available goals are:

```
 * clean                - clean local build tree
 * install              - build, run static analysis and unit tests, then install in the local repository
 * install-notests      - same as 'install', but skip unit tests
 * install-nodocker     - same as 'install', but skip unit tests that require a local docker installation
 * install-fast         - same as 'install', but skip unit tests and static analysis
 * compare-reproducible - compare against installed jars to ensure reproducible build
 * tests                - build code and run unit and integration tests except really slow tests
 * docs                 - build up-to-date documentation in docs/target/generated-docs/
 * run-tests            - run all unit and integration tests except really slow tests
 * run-slow-tests       - run all unit and integration tests
 * run-tests-nodocker   - same as 'run-tests', but skip all tests that require a local docker installation
```

- If you make changes to the Jdbi code, please run `make tests` before opening a PR.
- If you make changes to the documentation, please run `make docs` before opening a PR.

If you do not have a local docker installation (required for some tests), use the equivalent `-nodocker` goals.

| Make command           | function                                      | equivalent Apache Maven command                                                                                   |
|------------------------|-----------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `clean`                | clean local build tree                        | `mvn clean`                                                                                                       |
| `install`              | standard build command                        | `mvn clean install`                                                                                               |
| `install-notests`      | build without unit tests                      | `mvn -Dbasepom.test.skip=true clean install`                                                                      |
| `install-nodocker`     | build without docker                          | `mvn -Dno-docker=true clean install`                                                                              |
| `install-fast`         | build without tests and checkers              | `mvn -Pfast clean install`                                                                                        |
| `compare-reproducible` | compare a build against local install         | `mvn -Dbasepom.test.skip=true -Djdbi.check.skip-japicmp=true clean verify artifact:compare`                       |
| `tests`                | install and run tests                         | combination of `install-notests` and `run-tests`                                                                  |
| `docs`                 | build jdbi docs                               | `mvn -Ppublish-docs -Pfast -Dbasepom.javadoc.skip=false clean install`                                            |
| `run-tests`            | run unit and integration tests                | `mvn -Dbasepom.it.skip=false surefire:test invoker:install invoker:integration-test invoker:verify`               |
| `run-slow-tests`       | run testcontainer based tests                 | `mvn -Pslow-tests -Dbasepom.it.skip=false surefire:test invoker:install invoker:integration-test invoker:verify`  |
| `run-tests-nodocker `  | run unit and integration tests without docker | `mvn -Dno-docker=true surefire:test invoker:install invoker:integration-test invoker:verify`                      |

#### IntelliJ IDEA

* [IntelliJ IDEA formatting rules](https://github.com/jdbi/jdbi/blob/master/ide-support/intellij/jdbi.xml).

#### Eclipse IDE

* [Eclipse formatting rules](https://github.com/jdbi/jdbi/blob/master/ide-support/eclipse/jdbi-eclipse-formatter.xml).

Import these settings via Preferences &rarr; Java &rarr; Code Style &rarr; Formatter &rarr; Import... and activate them for all Jdbi modules.

* [Eclipse import order](https://github.com/jdbi/jdbi/blob/master/ide-support/eclipse/jdbi-eclipse.importorder).

```
#Organize Import Order
0=java
1=javax
2=
3=\#java
4=\#javax
5=\#
```

* Open your project's properties
* Go to Preferences &rarr; Java &rarr; Code Style &rarr; Organize Imports
* Click the *Import...* button and select the file you previously created
* Set both text boxes *Number of [static] imports needed for .* * to a large value such as 1000, effectively turning wildcard/star imports off
* Close the dialog
* Reorganize imports of modified source files using these rules before any commit

### Java Import Statements

We enforce this order of imports:

```
java.*

javax.*

*

static java.*

static javax.*

static *
```

A blank line is required between each group. Imports in a group must be ordered alphabetically.

Wildcard (aka star) imports e.g. `import org.apache.*;`, including static imports, are not allowed.

Javadoc may not cause an import statement i.e. use fully qualified class names (FQCN) in Javadoc unless the class was already imported by code.

### Enable `-parameters` compiler flag

Most of our SQL Object tests rely on SQL method parameter names. However by default, `javac` does not compile these
parameter names into `.class` files. Thus, in order for unit tests to pass, the compiler must be configured to output
parameter names.

#### IntelliJ IDEA

* File &rarr; Settings
* Build, Execution, Deployment &rarr; Compiler &rarr; Java Compiler
* Additional command-line parameters: `-parameters`
* Click Apply, then OK.
* Build &rarr; Rebuild Project

#### Eclipse IDE

* Window &rarr; Preferences &rarr; Java &rarr; Compiler
* Section *Classfile Generation*
* Check box *Add variable attributes to generated class files (used by the debugger)*
* Check box *Store information about method parameters (usable via reflection)*
* Click Apply and close dialog
