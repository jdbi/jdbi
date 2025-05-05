# Jdbi 3 - GraalVM Demo

This example is a small application that demonstrates the use of the Jdbi 3 framework with GraalVM to compile to native code.

It must be compiled with a GraalVM distribution (Tested with `Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 21.0.7+8.1 (build 21.0.7+8-LTS-jvmci-23.1-b60, mixed mode, sharing)` on MacOS 14.7 / arm64).

Run `mvn clean verify`. This creates the following binaries in the `target` folder:

- `jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar` - The application, repacked as a single jar
- `jdbi3-graalvm-jit-executable`                 - An executable shell script that will run the app with the regular `java` byte code
- `jdbi3-graalvm-aot-executable`                 - Native compiled application

The jar can be executed by running `java -jar target/jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar`. The two executables can be run directly from the command line.


## Native compilation

To compile an application that uses Jdbi 3, it is necessary to 'hint' the compilation process this can be done with a set of files in `META-INF/native-image` on the classpath. For this application, the files were created by running the JIT application with the native agent enabled:

``` bash
$ java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image,config-write-period-secs=30,config-write-initial-delay-secs=5 -jar target/jdbi3-graalvm-demo-1.0-SNAPSHOT-repacked.jar
```


## Java flight recorder

The [Java Flight Recorder](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH170) is a lightweight tool to collect diagnostic and profiling data from JVM applications. JFR is supported with Jdbi and can be used in native applications.

To compile the native application with Flight recorder support, run `mvn -Pflight-recorder clean verify`.

The flight recorder can be activated with `jdbi3-graalvm-aot-executable -XX:+FlightRecorder -XX:StartFlightRecording="filename=recording.jfr"`.
