Hi! Welcome to Jdbi.

We're glad you're thinking about contributing to the project.

Here's a few pointers to help you get set up:

# Policies

## Backward compatibility

Jdbi places serious emphasis on not breaking compatibility. Remember these simple rules and think twice before making any classes or class members `public`!

1) what comes into the API, stays in the API (or: no is temporary, but yes is forever);
2) if a piece of API must be discouraged after public release, mark it `@Deprecated` and keep it functionally intact;
3) breaking cleanup work can be done when Jdbi is gearing up for a major version number increment (see [SemVer](https://semver.org/));
4) bug fixes that **absolutely require** an API change are the only exception.

If you must make some internal code `public` to access it from other packages, put the class in a package named `internal`. Packages named so are not considered API.

### Forward compatibility

Completely new API should, in most cases, be marked with `@Beta`. This lets users know not to rely too much on your changes yet, as the public release might reveal that more work needs to be done.

## Technical design

We favor constructors — especially the default one — over factory methods where possible. Adding factory methods is not discouraged, but restricting the visibility of useful constructors without technical reason is.

Remember to implement thread safety wherever objects are likely to be shared between threads, but don't implement it where it definitely isn't needed. Making objects stateless or immutable is strongly encouraged!

### Testing

Unit tests are nice for atomic components, but since jdbi is a complex ecosystem of components, we prefer to use tests that spin up real jdbi instances and make it work against an in-memory database. This ensures all code is covered by many different test cases and almost no flaw will go unnoticed.

Since our tests essentially describe and verify jdbi's behavior, changing their specifics where it isn't inherently necessary is considered a red flag.

## Functionality

Jdbi should be useful for as many projects as possible with as little work as possible, within reason. It should be useful out of the box with sane defaults, but always configurable to the extent users are likely to need.

# Enable `-parameters` compiler flag in your IDE:

Most of our SQL Object tests rely on SQL method parameter names. However by default, `javac` does not compile these
parameter names into `.class` files. Thus, in order for unit tests to pass, the compiler must be configured to output
parameter names.

## IntelliJ

* File -> Settings
* Build, Execution, Deployment -> Compiler -> Java Compiler
* Additional command-line parameters: `-parameters`
* Click Apply, then OK.
* Build -> Rebuild Project
