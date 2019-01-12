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

### IntelliJ

* File &rarr; Settings
* Build, Execution, Deployment &rarr; Compiler &rarr; Java Compiler
* Additional command-line parameters: `-parameters`
* Click Apply, then OK.
* Build &rarr; Rebuild Project
