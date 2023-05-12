# Build tree layout

The build tree for Jdbi is multiple layers deep and organized
specifically so that it is possible to build some modules outside this
tree and still release with the same version numbers.

The pom inheritance model is:

``` text
                      basepom-oss (org.basepom:basepom-oss)
                                        |
                                        v
      +------- jdbi3-root (org.jdbi.internal:jdbi3-root) ------+
      |                                                        |
      |                                                        |
      |                                                        v
      |                            jdbi3-policy (org.jdbi.internal:jdbi3-policy)
      |                                                        .
      |                                  .......................
      |                                  .
      v                                  v
    jdbi3-build-parent (org.jdbi:jdbi3-build-parent) ---------------------------------+-- ... --> external
                           |                                                          |           modules
                           v                                                          |
   +--+--+--+----  jdbi3-parent (org.jdbi.internal:jdbi3-parent) .................    |
   |  |  |  |                                  |                                 .    |
   v  v  v  v                                  v                                 v    v
  ... other code modules ...          ... folder module ...                    jdbi3-bom
                                                                          (org.jdbi:jdbi3-bom)
```

* basepom-oss sets the maven plugins up
* jdbi3-root is the starting point for all other pom files.
* jdbi3-build-parent, which consumes the jdbi3-policy jar, and defines the Jdbi specific build policies
* jdbi3-parent contains the module definitions for the main Jdbi build.
* the jdbi3-bom is referenced from the jdbi3-parent, therefore it must inherit from the jdbi3-build-parent to avoid a loop.
* folder modules organize the build tree (e.g. for the internal modules or the cache modules). They are used by the build but are not deployed or installed.


This inheritance model is mapped onto the build tree in this folder structure:

``` text
/ jdbi3-parent (org.jdbi.internal:jdbi3-parent) -+- internal (org.jdbi.internal:jdbi3-internal-parent) -+- root (org.jdbi.internal:jdbi3-root)
                                                 |                                                      |
                                                 |                                                      +- build (org.jdbi:jdbi3-build-parent)
                                                 |                                                      |
                                                 |                                                      +- policy (org.jdbi.internal:jdbi3-policy)
                                                 +- bom (org.jdbi.internal:jdbi3-bom)
                                                 |
                                                 +-
                                                 +- ... all other code modules
                                                 +-
                                                 +-
                                                 +- cache-internal (org.jdbi.internal:jdbi3-internal-cache-parent) -+- ... cache modules ...
```

All modules with the `org.jdbi.internal` group id are solely for Jdbi
builds. Consuming them outside Jdbi builds is at your own risk, these
artifacts may change, move around or completely disappear if
necessary.


## Third party Jdbi modules

Outside build modules should be setup like this:

``` text
  jdbi3-build-parent (org.jdbi:jdbi3-build-parent)
                           |
                           v
         external build module (org.jdbi:jdbi3-foo)

```

This ensures that the jdbi3-foo module can be build and released off the Jdbi main release (and can use the same or different version numbers).


External projects should use this in their root pom files:

``` xml
<project xmlns="https://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>jdbi3-build-parent</artifactId>
        <groupId>org.jdbi</groupId>
        <version>[... current Jdbi release or snapshot version ...]</version>
    </parent>

    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-foo</artifactId>
    <version>[... current version of jdbi-foo ...]</version>

    <!-- referencing jdbi code -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.jdbi</groupId>
                <artifactId>jdbi3-bom</artifactId>
                <version>[... current Jdbi release or snapshot version ...]</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```
