# Build tree layout

Because it should be possible to build some modules outside this tree and still release with the same version numbers (cough, Oracle, cough), the build tree for JDBI is a bit "backwards".

The pom inheritance model is:

``` text
      +------------------------  basepom-oss ----------------------------+
      |                                                                  |
      |                                                                  |
      |                                                                  v
      |                                                        jdbi3-policy (org.jdbi.internal:jdbi3-policy)
      |                                                                  .
      |                                            .......................
      |                                            .
      v                                            v
  jdbi3-build-parent (org.jdbi.internal:jdbi3-build-parent) --------------------------+
                           |                                                          |
                           v                                                          |
   +--+--+--+----  jdbi3-parent (org.jdbi:jdbi3-parent) ..........................    |
   |  |  |  |                                  |                                 .    |
   v  v  v  v                                  v                                 v    v
  ... other code modules ...             jdbi3-internal-parent                  jdbi3-bom
                              (org.jdbi.internal:jdbi3-internal-parent)    (org.jdbi:jdbi3-bom)
```

* basepom-oss sets the maven plugins up
* jdbi3-build-parent, which consumes the jdbi3-policy jar, and defines the JDBI specific build policies
* jdbi3-parent contains the module definitions for the main JDBI build.
* the jdbi3-bom is referenced from the jdbi3-parent, therefore it must inherit from the jdbi3-build-parent to avoid a loop.


This inheritance model is mapped onto the build tree like this:

``` text
/ jdbi3-parent (org.jdbi:jdbi3-parent) -+- internal (org.jdbi.internal:jdbi3-internal-parent) -+- build (org.jdbi.internal:jdbi3-build-parent)
                                        |                                                      |
                                        |                                                      +- policy (org.jdbi.internal:jdbi3-policy)
                                        |
                                        +- bom (org.jdbi.internal:jdbi3-bom)
                                        |
                                        +-
                                        +- ... all other code modules
                                        +-
                                        +-
```

All modules with the `org.jdbi.internal` group id are solely for JDBI builds. Consuming them outside JDBI builds is at your own risk, these artifacts may change, move around or completely disappear if necessary.


## Third party JDBI modules

Outside build modules should be setup like this:

``` text
  jdbi3-build-parent (org.jdbi.internal:jdbi3-build-parent)
                           |
                           v
         external build module (org.jdbi:jdbi3-foo)

```

This ensures that the jdbi3-foo module can be build and released off the JDBI main release (and can use the same or different version numbers).


External projects should use this in their root pom files:

``` xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <artifactId>jdbi3-build-parent</artifactId>
        <groupId>org.jdbi.internal</groupId>
        <version>[... current JDBI release or snapshot version ...]</version>
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
                <version>[... current JDBI release or snapshot version ...]</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```
