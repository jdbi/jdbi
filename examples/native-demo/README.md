# Jdbi 3 - GraalVM Demo

This example is a small application that demonstrates the use of the Jdbi 3 framework with GraalVM to compile to native code.

It must be compiled with a GraalVM distribution. Tested with Oracle GraalVM 25 on macOS/arm64 and Linux/aarch64.

Run `mvn clean verify`. This creates the following binaries in the `target` folder:

- `jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar` - The application, repacked as a single jar
- `jdbi3-graalvm-jit-executable`                 - An executable shell script that will run the app with the regular `java` byte code
- `jdbi3-graalvm-aot-executable`                 - Native compiled application

The jar can be executed by running `java -jar target/jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar`. The two executables can be run directly from the command line.

The demo pauses between queries, so the default run of 1000 queries takes several minutes. Pass a query count to shorten it: `jdbi3-graalvm-aot-executable 20`.


## Native compilation

Native compilation needs reachability metadata: a `reachability-metadata.json` under `META-INF/native-image/<groupId>/<artifactId>/` on the classpath, listing what the application reaches by reflection, through a proxy, or as a resource. This demo's file is short because most of it is supplied already. Jdbi ships metadata for its own classes in the `jdbi3-core` and `jdbi3-sqlobject` jars (since 3.50.0), the GraalVM reachability metadata repository covers H2, and `native-image` registers much of the JDK itself.

What Jdbi ships covers Jdbi's own classes, not yours. An application registers the types Jdbi reaches on its behalf: the types a mapper constructs, the SQL Object interfaces it attaches, and any enum it binds or maps. An enum needs an entry of its own, because Jdbi resolves a constant by name through a field lookup:

``` json
{"type":"com.example.Colour"}
```

The tracing agent writes a first draft of the file:

``` bash
$ java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/org.jdbi/jdbi3-graalvm-demo,config-write-period-secs=30,config-write-initial-delay-secs=5 -jar target/jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar
```

Treat that output as a starting point rather than an answer: it records whatever the run happened to touch, including entries the layers above already cover and JDK internals that will not survive a JDK upgrade. To establish what is actually required, strip the file back to the application's own types and run the image with `-XX:MissingRegistrationReportingMode=Warn`, which reports every missing registration in one pass instead of aborting at the first. Whatever it names is required. That mode continues past each problem and can still exit 0, so confirm the result with an ordinary run afterwards. To test whether one entry is load-bearing, delete it, rebuild, and run: do not infer it from Jdbi's source. A lookup `native-image` can resolve while building the image is answered there, and answering it registers that type for every later lookup, so of the qualifiers Jdbi synthesizes internally, `EnumByName` and `NVarchar` need no entry while `Legacy` and `EncodedJson` do, from identical-looking code.

The build passes `--exact-reachability-metadata` so that an unregistered lookup throws. The default is to answer it as absent, which callers cannot tell from a legitimate empty result: Jdbi ignores `ReflectiveOperationException` in places, and slf4j-simple reads a missing config file as "no configuration". A gap in the metadata therefore produces no error, only different behavior. That is also why `simplelogger.properties` is registered although the demo ships no such file: registering something absent is legal, and is how such a lookup is satisfied. The two `org.jdbi.v3` entries are here only because the demo builds against 3.54.0, which predates them in `jdbi3-core`. They ship in core from 3.54.1 on, so drop them when `dep.jdbi3.version` moves past 3.54.0.


## Java flight recorder

The [Java Flight Recorder](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH170) is a lightweight tool to collect diagnostic and profiling data from JVM applications. JFR is supported with Jdbi and can be used in native applications.

To compile the native application with Flight recorder support, run `mvn -Pflight-recorder clean verify`.

The flight recorder can be activated with `jdbi3-graalvm-aot-executable -XX:+FlightRecorder -XX:StartFlightRecording="filename=recording.jfr"`.
