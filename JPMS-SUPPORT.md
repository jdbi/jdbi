# Jdbi and the Java Platform Module System (JPMS)

JPMS was introduced in Java 9 as a a solution to the "jar hell" that
exists with projects putting pieces on the class path and hoping that
the JVM will figure out how to load the right things at the right
moment.

JPMS has its champions and a fair share of criticism
(e.g. https://blog.joda.org/2017/05/java-se-9-jpms-automatic-modules.html)
and that is besides the point. Java has shipped it as part of the
platform and it is expected that libraries start using it.


## State of things (as of 3.40.1)

Jdbi introduced basic modularization in 2019 (see
https://github.com/jdbi/jdbi/issues/812) and has shipped basic JPMS
module support (having automatic module names) since release 3.7.1.

Shipping automatic module names is sufficient to allow other projects
and libraries to declare module dependencies on Jdbi artifacts. Jdbi
uses `org.jdbi.v3.<...>` as automatic module names. By having
automatic module names for all artifacts, Jdbi avoids having the JVM
create dummy module names from the jar names which is unstable.

Build warnings like `src/main/java/module-info.java:[14,16] module
name component v3 should avoid terminal digits` are here
forever. Those are annoying but not critical.


## Javadocs

With the release of maven-javadoc-plugin 3.6.0, we can build non-JPMS
javadocs again that use Java 11 (our release version) and no longer
report warnings.

Going forward, while it is possible to build per-maven module javadocs
that are structured like java 9+ javadocs, building an aggregation
(all javadocs in one place) has consistently failed.

For aggregated jars, the way the maven-javadoc plugin calls the
javadoc tool does not work ("too many patched modules") or limitations
of the maven-javadoc-plugin
( see https://issues.apache.org/jira/browse/MJAVADOC-768, unresolved)

In addition, the javadoc tool itself does not work well with automatic
modules (the --module-source-path option only works with module
descriptors, so patching the module with the source code is the only
viable option).

## Full modularization

Using the [moditect JPMS
plugin](https://github.com/moditect/moditect), it is possible to put
together a basic set of module descriptors that allow the code to
compile. However, as the module descriptors are added after
compilation, there is no guarantee that the code actually works.


### File-name based dependencies

Jdbi has a large number of external dependencies. Migrating a project to JPMS needs to be done "bottom-up" [https://blog.joda.org/2017/05/java-se-9-jpms-automatic-modules.html] to avoid "module hell".

JPMS modules come in three flavors:

- modules with module-descriptors. Depending on those is fine and they will end up on the module path.
- modules with an Automatic-Module-Name in the manifest. Depending on those is acceptable, as their module name is stable and was set intentionally.
- everything else. Module names for these jars is set by the filename. Those modules are unstable (people may add manifest entries or module descriptors *with a different* module name. Depending on those and releasing publicly is a risk. Maven actually warns about those:

```
[INFO] --- compiler:3.11.0:compile (default-compile) @ jdbi3-testing ---
[WARNING] *************************************************************************************************************************************************
[WARNING] * Required filename-based automodules detected: [otj-pg-embedded-1.0.1.jar]. Please don't publish this project to a public artifact repository! *
[WARNING] *************************************************************************************************************************************************
```


More time needs to be spent on analyzing these dependencies.


### Building Jdbi with JPMS support

Jdbi, in its current shape, serves our users well. Bringing in JPMS should not disrupt this and it should also not make the development process cumbersome.

Explicit requirements for this transition:

- Keeping the maven structure of "main artifact" / "test artifact" with the same packages per maven module. This allows using package scope visibility to expose test code and does not pollute the public API.
- It must be possible to use "test artifacts" in other maven modules in the same way as the current build does. Having to refactor the code base to accomodate JPMS should be avoided.
- Allow the use of provided and optional dependencies in the maven dependency graph. Module descriptors should be able to deal with optional dependencies and the testing code must support it.


This has yielded a number of problems:

- https://github.com/codehaus-plexus/plexus-languages/pull/164. There is a workaround in place for this (https://github.com/jdbi/jdbi/commit/a2d0e651662fbb7e58d7c51da21b410422d62206).
- https://issues.apache.org/jira/browse/SUREFIRE-2190 (fix merged, but surefire has not been released since it was merged)
- https://issues.apache.org/jira/browse/SUREFIRE-2191 is a blocker for the testing module


The current idea is to build the main artifacts as a JPMS module but
leave the testing code as Automatic-Module-Name artifacts.

Using module descriptors for both the main and test artifact creates
multiple modules with the same package, which is not allowed as
multiple modules can not have overlapping package space (this turns
out to be a major blocker for a lot of maven projects).

There is no good answer to this, short of packaging the test code
within the main artifacts to allow execution in a single JPMS
module. The current surefire tooling works around this by patching the
test classes into the main artifact using `--patch-module`.

The testing dependency graph is error prone and brittle with more
complicated dependency sets.


## Conclusion

Moving Jdbi to fully support JPMS will be difficult unless major
changes in the structure of the code base are acceptable. There are
some proposals (e.g. https://github.com/jdbi/jdbi/pull/2453, which
focuses on compilation) but there is no good path forward right now.

Putting the focus on getting tooling in place that allows us to ship
Java 11 compatible javadocs. Adressing the different issues with
building and testing the code base will take more time.

There also needs to be a push to get as many file name based automatic
modules out of the dependency graph as possible. Depending on the
responsiveness of the various project, some code may need to be
deprecated and removed.
