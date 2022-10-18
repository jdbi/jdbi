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

Completely new API should, in most cases, be marked with `@Beta`. This lets users know not to rely too much on your changes yet, as the public release might reveal that more work needs to be done.

## Functionality

Jdbi should be useful for as many projects as possible with as little work as possible, within reason. It should be useful out of the box with sane defaults, but always configurable to the extent users are likely to need.

## Technical design

We like both constructors and factory methods/builders, but require that they be used appropriately. Constructors are great for dumb classes, factories are better in case any defensive logic is involved.

Remember to implement thread safety wherever objects are likely to be shared between threads, but don't implement it where it definitely isn't needed. Making objects stateless or immutable is strongly encouraged!

## Testing

Unit tests are nice for atomic components, but since jdbi is a complex ecosystem of components, we prefer to use tests that spin up real jdbi instances and make it work against an in-memory database. This ensures all code is covered by many different test cases and almost no flaw will go unnoticed.

Since our tests essentially describe and verify jdbi's behavior, changing their specifics where it isn't inherently necessary is considered a red flag.

## Pull requests

Expect your pull request to be scrutinized in even the tiniest details (down to grammar in javadoc), and to need to address many remarks even for small changes. We strive for a healthy balance between subjective perfection and practical considerations, but we are firmly against doing a quick and sloppy job that will require a lot of follow-up work later.

Due to the volume of feedback in a typical PR, we may push changes directly to your PR branch if we are able to, in order to save time and frustration for everyone.

# Setup

## Enable `-parameters` compiler flag

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

## Code Style / Formatting Rules

The project uses a set of code style and formatting rules. These are enforced by Checkstyle and PMD as part of the build cycle.
We ask that you configure the project in your development environment to support these rules as closely as possible.

The JDBI Eclipse formatting preferences are available for download [here](internal/policy/src/main/resources/ide/jdbi-eclipse-formatter.xml).

### IntelliJ IDEA

Import the file with the 'Adapter for Eclipse Code Formatter'. Additional info is available [here](https://plugins.jetbrains.com/plugin/6546-adapter-for-eclipse-code-formatter).

### Eclipse IDE

Import these settings via Preferences &rarr; Java &rarr; Code Style &rarr; Formatter &rarr; Import... and activate them for all JDBI modules.

## Java Import Statements

We enforce this order of imports:

```
java.*
javax.*
*
static java.*
static javax.*
static *
```

A blank line is required between each group.
Imports in a group must be ordered alphabetically.

Wildcard (aka star) imports e.g. `import org.apache.*;`, including static imports, are not allowed.

Javadoc may not cause an import statement i.e. use FQCN in Javadoc unless the import statement is already caused by code.

### Eclipse IDE

* Save this content to a text file named `jdbi-eclipse.importorder`
  or download from [here](internal/policy/src/main/resources/ide/jdbi-eclipse.importorder)

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

