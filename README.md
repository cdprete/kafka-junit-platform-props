[gradle-url]: https://gradle.org/
[kafka-url]: https://github.com/apache/kafka
[junit-url]: https://github.com/junit-team/junit5
[junit-cnf-url]: https://junit.org/junit5/docs/current/user-guide/#writing-tests-test-instance-lifecycle-changing-default
[mvn-test-jar]: https://maven.apache.org/components/plugins/maven-jar-plugin/examples/create-test-jar.html

# Multiple `junit-platform.properties` on the classpath

## Problem
The `test-test` JARs from [Apache Kafka][kafka-url] contain a [`junit-platform.properties`][junit-cnf-url] which 
clashes with the one present in the project.  

> The JARs are named `test-test` because their scope in the project is `test` and they contain the `test-classes` (as 
> well as the test resources) of the project they come from as described in [Apache Maven JAR Plugin][mvn-test-jar] 
> documentation.
> 
> For [Gradle][gradle-url] the concept is similar.

As such, [JUnit][junit-url] issues a `WARNING` during the execution of the test(s) claiming that only the first one 
will be used.

```text
Aug 09, 2024 4:35:50 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
Aug 09, 2024 4:35:51 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
Aug 09, 2024 4:35:51 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
```

As such, depending on the order used by the classloader, the one present in the project may not be used leading to 
problems and/or impacting the performances if the test(s) relies on that configuration.

> The issue is also tracked on the JUnit issue tracker at https://github.com/junit-team/junit5/issues/2794.

## Find the culprits
In order to find the JARs causing the issue, a dummy test has been written that just scans the classpath and prints all 
the occurrences of the `junit-platform.properties` file in the classpath.
> The code of the test comes from https://github.com/junit-team/junit5/issues/2207#issue-576763797.

To run the test, execute the command:
```shell
mvn clean test
```

An output like the following should be printed, showing that there are multiple `junit-platform.properties` files on 
the classpath:
```text
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------< org.example:kafka-junit-platform-props >---------------
[INFO] Building kafka-junit-platform-props 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.2.0:clean (default-clean) @ kafka-junit-platform-props ---
[INFO] Deleting C:\Users\Cosimo Damiano Prete\IdeaProjects\kafka-junit-platform-props\target
[INFO]
[INFO] --- resources:3.3.1:resources (default-resources) @ kafka-junit-platform-props ---
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO]
[INFO] --- compiler:3.13.0:compile (default-compile) @ kafka-junit-platform-props ---
[INFO] Nothing to compile - all classes are up to date.
[INFO]
[INFO] --- resources:3.3.1:testResources (default-testResources) @ kafka-junit-platform-props ---
[INFO] Copying 1 resource from src\test\resources to target\test-classes
[INFO]
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ kafka-junit-platform-props ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 1 source file with javac [debug target 21] to target\test-classes
[INFO] 
[INFO] --- surefire:3.2.5:test (default-test) @ kafka-junit-platform-props ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
Aug 09, 2024 4:50:58 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
Aug 09, 2024 4:50:59 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
Aug 09, 2024 4:50:59 PM org.junit.platform.launcher.core.LauncherConfigurationParameters loadClasspathResource
WARNING: Discovered 3 'junit-platform.properties' configuration files in the classpath; only the first will be used.
[INFO] Running ClassPathTest

loader = jdk.internal.loader.ClassLoaders$AppClassLoader@70dea4e
loader.parent = jdk.internal.loader.ClassLoaders$PlatformClassLoader@10163d6
loader.parent.parent = null

single: file:.../IdeaProjects/kafka-junit-platform-props/target/test-classes/junit-platform.properties

multi: file:.../IdeaProjects/kafka-junit-platform-props/target/test-classes/junit-platform.properties
multi: jar:file:.../.m2/repository/org/apache/kafka/kafka-clients/3.7.1/kafka-clients-3.7.1-test.jar!/junit-platform.properties
multi: jar:file:.../.m2/repository/org/apache/kafka/kafka-server-common/3.7.1/kafka-server-common-3.7.1-test.jar!/junit-platform.properties

distinct: jar:file:.../.m2/repository/org/apache/kafka/kafka-server-common/3.7.1/kafka-server-common-3.7.1-test.jar!/junit-platform.properties
distinct: file:.../IdeaProjects/kafka-junit-platform-props/target/test-classes/junit-platform.properties
distinct: jar:file:.../.m2/repository/org/apache/kafka/kafka-clients/3.7.1/kafka-clients-3.7.1-test.jar!/junit-platform.properties
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in ClassPathTest
[INFO] 
[INFO] Results:
[INFO]
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.774 s
[INFO] Finished at: 2024-08-09T16:50:59+02:00
[INFO] ------------------------------------------------------------------------
```

> The starting point of the paths has been removed for privacy reasons.

The output shows that the additional properties files are coming from the `test-test` JARs since the JAR names follow 
the pattern `<name>-<version>-test.jar`.

## Possible solutions
The ideal solution would be to remove the `test-test` JAR files.  
If this is not possible, it would already be beneficial to remove the `junit-platform.properties` file from them.