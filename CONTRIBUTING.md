![Jdbi Logo](docs/src/adoc/images/logo.svg)

Hi! Welcome to Jdbi.

We're glad you're thinking about contributing to the project.

Here's a few pointers to help you get set up:

# Policies

## Compatibility

### Backward

Jdbi places serious emphasis on not breaking compatibility. Remember these simple rules and think twice before making any classes or class members `public`!

1) what comes into the API, stays in the API (or: no is temporary, but yes is forever);
2) if a piece of API must be discouraged after public release, mark it `@Deprecated` and keep it functionally intact;
3) breaking cleanup work can be done when Jdbi is gearing up for a major version number increment (see [SemVer](https://semver.org/));
4) bug fixes that **absolutely require** an API change are the only exception.

If you must make some internal code `public` to access it from other packages, put the class in a package named `internal`. Packages named so are not considered API.

### Forward

Completely new APIs should be marked with [@Alpha](https://jdbi.org/apidocs/org/jdbi/v3/meta/Alpha.html) or [@Beta](https://jdbi.org/apidocs/org/jdbi/v3/meta/Beta.html). This lets users know not to rely too much on your changes yet, as the public release might reveal that more work needs to be done.

## Functionality

Jdbi should be useful for as many projects as possible with as little work as possible, within reason. It should be useful out of the box with sane defaults, but always configurable to the extent users are likely to need.

## Technical design

We like both constructors and factory methods/builders, but require that they be used appropriately. Constructors are great for dumb classes, factories are better in case any defensive logic is involved.

Remember to implement thread safety wherever objects are likely to be shared between threads, but don't implement it where it definitely isn't needed. Making objects stateless or immutable is strongly encouraged!

## Testing

Unit tests are nice for atomic components, but since jdbi is a complex ecosystem of components, we prefer to use tests that spin up real jdbi instances and make it work against an in-memory database. This ensures all code is covered by many different test cases and almost no flaw will go unnoticed.

Since our tests essentially describe and verify jdbi's behavior, changing their specifics where it isn't inherently necessary is considered a red flag.

The use of mocks and mocking framework is generally discouraged. There are a number of existing tests that use Mockito and they are a pain to maintain.

## Pull requests

We strive for a healthy balance between subjective perfection and practical considerations, but we are firmly against doing a quick and sloppy job that will require a lot of follow-up work later.

Due to the volume of feedback in a typical PR, we may push changes directly to your PR branch if we are able to, in order to save time and frustration for everyone.

# Development Setup

Most modern IDEs configure themselves correctly by importing the Jdbi repository as an Apache Maven project. If necessary, install support for Apache Maven first.

## Code Style / Formatting Rules

The project uses a set of code style and formatting rules. These are enforced by Checkstyle and PMD as part of the build cycle.

We do not review or merge PRs that do not pass our pre-merge checks. Run the build locally using `make install`.

## Make driven build

There is a Makefile at the root of the project to drive the various builds. Run `make` or `make help` to display all available goals. Some goals are privileged (you need to be a member of the Jdbi development team). Generally available goals are:

```
 * clean            - clean local build tree
 * install          - builds and installs the current version in the local repository
 * install-nodocker - same as 'install', but skip all tests that require a local docker installation
 * install-fast     - same as 'install', but skip test execution and code analysis (Checkstyle/PMD/Spotbugs)
 * docs             - build up-to-date documentation in docs/target/generated-docs/
 * tests            - run all unit and integration tests
 * tests-nodocker   - same as 'tests', but skip all tests that require a local docker installation
```

- If you make changes to the Jdbi code, please run `make install` before opening a PR.
- If you make changes to the documentation, please run `make docs` before opening a PR.

If you do not have a local docker installation (required for some tests), use the equivalent `-nodocker` goals.


### IntelliJ IDEA

* [IntelliJ IDEA formatting rules](https://github.com/jdbi/jdbi/blob/master/ide-support/intellij/jdbi.xml).

### Eclipse IDE

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

### IntelliJ IDEA

* File &rarr; Settings
* Build, Execution, Deployment &rarr; Compiler &rarr; Java Compiler
* Additional command-line parameters: `-parameters`
* Click Apply, then OK.
* Build &rarr; Rebuild Project

### Eclipse IDE

* Window &rarr; Preferences &rarr; Java &rarr; Compiler
* Section *Classfile Generation*
* Check box *Add variable attributes to generated class files (used by the debugger)*
* Check box *Store information about method parameters (usable via reflection)*
* Click Apply and close dialog
