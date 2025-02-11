# Recent changes

### Java 17 Support
To support Java 17, we needed to add support for the `RUNTIME_VERSION` option when creating procedures and functions with JVM languages.
The `runtime` parameter was added to the `applications` DSL to support this change, but the default value will stay `11` for now to align with the Snowflake default.

Additionally, Java 17 is also being used now to compile and build this plugin.

# Motivation

It needs to be easy to develop and test JVM applications even if they are being deployed to Snowflake.
Using [Gradle](https://www.gradle.org), we can easily build shaded JAR files with all dependencies included using
the [shadow plugin](https://imperceptiblethoughts.com/shadow/), and I've provided
a [sample Java project](examples/java-manual/) that demonstrates this basic use case:

```shell
cd examples/java-manual &&
./gradlew build
```

But this JAR would still have to be uploaded to a stage in Snowflake, and the UDF would have to be created or possibly
recreated if its signature changed.
I wanted an experience using Snowflake that is as natural to developers using IntelliJ or VS Code for standard JVM
projects.

# The Gradle Snowflake Plugin

This plugin provides easy configuration options for those getting started with Gradle but also provides advanced
features for teams already using Gradle in other areas of the organization.
It has three basic modes:

1. Lightweight publishing to internal Snowflake stages using Snowpark.
2. Slightly heavier publishing using external Snowflake stages and autoconfiguration of
   the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.
3. Publishing to Snowflake using external stages and custom configuration of
   the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

Have a look at
the [API docs](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/package-summary.html).

This plugin can be used to build UDFs and procedures in any JVM language supported by Gradle, which currently provides
official support
for Java, Scala, Kotlin and Groovy.
See the [examples](examples) directory for examples using different languages.

# Internal Stages using Snowpark

Unless you have a heavy investment in Gradle as an organization, this is likely the option you want to use.
Additionally, if you plan on *sharing* UDFs across Snowflake accounts, this is the option you *have* to use, as JARs
need to be in named internal stages.
Look at the [sample Java project using the plugin](examples/java/) and you'll notice a few differences in
the [build file](examples/java/build.gradle).

```shell
cd examples/java &&
cat build.gradle
```

We applied `io.github.stewartbryson.snowflake` and removed `com.github.johnrengelman.shadow` because the `shadow` plugin
is automatically applied by the `snowflake` plugin:

```groovy
plugins {
   id 'java'
   id 'io.github.stewartbryson.snowflake' version '2.1.22'
}
```

We now have the `snowflakeJvm` task available:

```
❯ ./gradlew help --task snowflakeJvm

> Task :help
Detailed task information for snowflakeJvm

Path
     :snowflakeJvm

Type
     SnowflakeJvm (io.github.stewartbryson.SnowflakeJvm)

Options
     --config     Custom credentials config file.

     --connection     Override the credentials connection to use. Default: use the base connection info in credentials config.

     --jar     Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.

     --stage     Override the Snowflake stage to publish to.

     --rerun     Causes the task to be re-run even if up-to-date.

Description
     A Cacheable Gradle task for publishing UDFs and procedures to Snowflake

Group
     publishing

BUILD SUCCESSFUL in 2s
5 actionable tasks: 3 executed, 2 up-to-date
```

Several command-line options mention _overriding_ other configuration values.
This is because the plugin also provides a DSL closure called `snowflake` that we can use to configure our build, which
is documented in
the [class API docs](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/SnowflakeExtension.html):

```groovy
snowflake {
   connection = 'gradle_plugin'
   stage = 'upload'
   applications {
      add_numbers {
         inputs = ["a integer", "b integer"]
         returns = "string"
         runtime = '17'
         handler = "Sample.addNum"
      }
   }
}
```

Snowflake credentials are managed in a config file, with the default being `~/.snowflake/config.toml` as prescribed by the [Snowflake Developer CLI](https://github.com/Snowflake-Labs/snowcli) project.
As a secondary location, we also support the [SnowSQL config](https://docs.snowflake.com/en/user-guide/snowsql-config) file.
In searching for a credentials config file, the plugin works in the following order:

1. A custom location of your choosing, configured with the `--config` option in applicable tasks.
2. `<HOME_DIR>/.snowflake/config.toml`
3. `<HOME_DIR>/.snowsql/config`
4. `./config.toml` (This is useful in CI/CD pipelines, where secrets can be written easily to this file.)

The `connection` property in the plugin DSL defines which connection to use from the config file, relying on the default values if none is
provided.
It first loads all the default values, and replaces any values from the connection, similar to how Snowflake CLI and SnowSQL work.

The nested 
[`applications` DSL](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/ApplicationContainer.html)
might seem a bit daunting.
This is a simple way to configure all the different UDFs and procedures we want to automatically create (or recreate)
each time we publish the JAR file.
The example above will generate and execute the following statement.
Notice that the `imports` part of the statement is automatically added by the plugin as the JAR (including our code and all dependencies) is automatically uploaded to Snowflake:

```roomsql
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@upload/libs/java-0.1.0-all.jar')
```

With the configuration complete, we can execute the `snowflakeJvm` task, which will run any unit tests (see _Testing_
below) and then publish
our JAR and create our function.
Note that if the named internal stage does not exist, the plugin will create it first:

```
❯ ./gradlew snowflakeJvm

> Task :snowflakeJvm
Using credentials config file: /Users/stewartbryson/.snowflake/config.toml
File java-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@upload/libs/java-0.1.0-all.jar')


BUILD SUCCESSFUL in 5s
7 actionable tasks: 2 executed, 5 up-to-date
```

Our function now exists in Snowflake:

```roomsql
select add_numbers(1,2);

Sum is: 3
```

The `snowflakeJvm` task was written to be [_incremental_](https://docs.gradle.org/current/userguide/incremental_build.html#incremental_build) and [_cacheable_](https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching_details).
If we run the task again without making any changes to task inputs (our code) or outputs, then the execution is avoided,
which we know because of the *up-to-date* keyword.

```
❯ ./gradlew snowflakeJvm

BUILD SUCCESSFUL in 624ms
3 actionable tasks: 3 up-to-date
```

This project uses the [S3 Gradle build cache plugin](https://github.com/burrunan/gradle-s3-build-cache)
and we are very happy with it.
Check the [Gradle settings file](settings.gradle) for an implementation example.

# Testing
Gradle
has [built-in Testing Suites for JVM projects](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html),
and using this functionality for JVM applications with Snowflake was my main motivation for writing this
plugin.
We'll now describe two topics regarding this plugin: _unit testing_ and _functional testing_.

### Unit Testing
Unit testing with Gradle is a dense topic, and the documentation will inform developers better than we can.
In general, unit tests are what developers write regardless of where the code eventually gets executed.
In the [Java with testing example](examples/java-testing), you can see a sample testing _specification (spec)_ using the
[Spock Framework](https://spockframework.org/) and written in Groovy, which is a requirement for that framework,
as is applying the `groovy` plugin to the project.

```shell
cd examples/java-testing
```

Our `plugins` DSL from the build file:
```groovy
plugins {
    id 'java'
    id 'groovy' // needed for Spock testing framework
    id 'io.github.stewartbryson.snowflake' version '2.1.22'
}
```

Our choice to use the Spock framework in the default `test` testing suite:
```groovy
test {
   useSpock('2.3-groovy-3.0')
}
```

And the unit test `SampleTest` spec in `src/test/groovy`:
```groovy
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class SampleTest extends Specification {
   @Shared
   @Subject
   def sample = new Sample()

   def "adding 1 and 2"() {
      when: "Two numbers"
      def a = 1
      def b = 2
      then: "Add numbers"
      sample.addNum(a, b) == "Sum is: 3"
   }

   def "adding 3 and 4"() {
      when: "Two numbers"
      def a = 3
      def b = 4
      then: "Add numbers"
      sample.addNum(a, b) == "Sum is: 7"
   }
}
```

All unit tests in either `src/test/java` (written using JUnit or something else) or `src/test/groovy` (written with Spock)
will automatically run whenever the `test` or `build` task is executed.

```shell
❯ ./gradlew build

> Task :test

SampleTest

  Test adding 1 and 2 PASSED
  Test adding 3 and 4 PASSED

SUCCESS: Executed 2 tests in 487ms

BUILD SUCCESSFUL in 1s
9 actionable tasks: 5 executed, 4 up-to-date
```

All Gradle testing tasks are automatically incremental and cacheable, and would be avoided if executed again without changes to the code in either the source or the spec.
The same applies for the topic of functional testing below.

### Functional Testing
[Functional testing](https://en.wikipedia.org/wiki/Functional_testing) describes what the system does,
and in my mind, this involves testing our deployed code in Snowflake. 
Regardless of what we call it, we _know we need this_ as a crucial component in our build chain.
This plugin contains a custom Spock Specification class called `SnowflakeSpec` that can be used in a new test suite.
By default, this test suite is called `functionalTest`, though the name can be configured using the `testSuite` property.

Here is our configuration of the `functionalTest` test suite.
The DSL provided by Gradle in JVM Testing is convoluted (in my opinion), but no one asked me:
```grovy
functionalTest(JvmTestSuite) {
   targets {
       all {
           useSpock('2.3-groovy-3.0')
           dependencies {
               implementation "io.github.stewartbryson:gradle-snowflake-plugin:2.1.22"
           }
           testTask.configure {
               failFast true
               // which credentials connection to use
               systemProperty 'connection', project.snowflake.connection
           }
       }
   }
}
```

I'll walk through a few of these points.
So that the `SnowflakeSpec` is available in the test classpath, we have to declare the plugin as a dependency
to the test suite.
Notice that we use the library maven coordinates, which are different from the coordinates in the `plugins` DSL.
Additionally, our test specs are unaware of all the configurations of our Gradle build, so we have to pass our `connection`
property as a Java system property to the `SnowflakeSpec` class.

This is the `SnowflakeSampleTest` spec in `src/functionalTest/groovy`:
```groovy
import groovy.util.logging.Slf4j
import io.github.stewartbryson.SnowflakeSpec

/**
 * The SnowflakeSpec used for testing functions.
 */
@Slf4j
class SnowflakeSampleTest extends SnowflakeSpec {

   def 'ADD_NUMBERS() function with 1 and 2'() {
      when: "Two numbers exist"
      def a = 1
      def b = 2

      then: 'Add two numbers using ADD_NUMBERS()'
      selectFunction("add_numbers", [a,b]) == 'Sum is: 3'
   }

   def 'ADD_NUMBERS() function with 3 and 4'() {
      when: "Two numbers exist"
      def a = 3
      def b = 4

      then: 'Add two numbers using ADD_NUMBERS()'
      selectFunction("add_numbers", [a,b]) == 'Sum is: 7'
   }

}
```
The `selectFunction` method is an easy way to execute a function and test the results by just passing the function name and a list of arguments to pass to that function.
And of course, this executes against Snowflake in real time.

```shell
❯ ./gradlew functionalTest

> Task :test

SampleTest

  Test adding 1 and 2 PASSED
  Test adding 3 and 4 PASSED

SUCCESS: Executed 2 tests in 481ms


> Task :snowflakeJvm
Using credentials config file: /Users/stewartbryson/.snowflake/config.toml
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@upload/libs/java-testing-0.1.0-all.jar')


> Task :functionalTest

SnowflakeSampleTest

  Test ADD_NUMBERS() function with 1 and 2 PASSED (1.1s)
  Test ADD_NUMBERS() function with 3 and 4 PASSED

SUCCESS: Executed 2 tests in 3.8s


BUILD SUCCESSFUL in 11s
11 actionable tasks: 7 executed, 4 up-to-date
```

### Testing with Ephemeral Database Clones
Running functional tests using static Snowflake databases is boring, especially considering
the [zero-copy cloning](https://docs.snowflake.com/en/user-guide/object-clone.html#cloning-considerations) functionality
available.
The plugin supports cloning an ephemeral database from the database we connect to and using it for testing our application.
This workflow is useful for CI/CD processes and is configured with the plugin DSL.
The plugin is aware when it is running in CI/CD environments and currently supports:
* [Travis CI](https://travis-ci.org)
* [Jenkins](https://jenkins.io)
* [GitLab CI](https://about.gitlab.com/product/continuous-integration/)
* [GitHub Actions](https://github.com/features/actions)
* [Appveyor](https://www.appveyor.com)

Because the plugin is aware when executing in CICD environments, we expose that information through the DSL, 
and can use it to control our cloning behavior.

Our build file change to enable ephemeral testing:
```groovy
snowflake {
   connection = 'gradle_plugin'
   stage = 'upload'
   useEphemeral = project.snowflake.isCI() // use ephemeral with CICD workflows
   keepEphemeral = project.snowflake.isPR() // keep ephemeral for PRs
   applications {
      add_numbers {
         inputs = ["a integer", "b integer"]
         returns = "string"
         runtime = '17'
         handler = "Sample.addNum"
      }
   }
}
```

The `useEphemeral` property will determine whether the `createEphemeral` and `dropEphemeral` tasks 
are added at the beginning and end of the build, respectively.
This allows for the `functionalTest` task to be run in the ephemeral clone just after our application is published.
We've also added a little extra magic to keep the clone when building a pull request.
The `createEphemeral` task issues a `CREATE DATABASE... IF NOT EXISTS` statement, so if will not fail if the clone exists from a prior run.
Remember that our `SnowflakeSpec` class doesn't automatically know the details of our build, so we have to provide
the ephemeral name using java system properties. Here is our modified testing suite:

```groovy
functionalTest(JvmTestSuite) {
   targets {
       all {
           useSpock('2.3-groovy-3.0')
           dependencies {
               implementation "io.github.stewartbryson:gradle-snowflake-plugin:1.1.4"
           }
           testTask.configure {
               failFast true
               // which credentials connection to use
               systemProperty 'connection', project.snowflake.connection
               // if this is ephemeral, the test spec needs the name to connect to
               if (project.snowflake.useEphemeral) {
                   systemProperty 'ephemeralName', snowflake.ephemeralName
               }
           }
       }
   }
}
```

We can simulate a GitHub Actions environment just by setting the `GITHUB_ACTIONS` environment variable:

```shell
❯ export GITHUB_ACTIONS=true
❯ ./gradlew functionalTest

> Task :createEphemeral
Using credentials config file: /Users/stewartbryson/.snowflake/config.toml
Ephemeral clone EPHEMERAL_JAVA_TESTING_PR_4 created.

> Task :snowflakeJvm
Reusing existing connection.
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@upload/libs/java-testing-0.1.0-all.jar')


> Task :functionalTest

SnowflakeSampleTest

  Test ADD_NUMBERS() function with 1 and 2 PASSED
  Test ADD_NUMBERS() function with 3 and 4 PASSED

SUCCESS: Executed 2 tests in 3s

BUILD SUCCESSFUL in 6s
13 actionable tasks: 4 executed, 9 up-to-date
```

When the CI/CD environment is detected, the plugin will name the ephemeral database clone based on the pull request
number, the branch name, or the tag name instead of an autogenerated one.
If we prefer to simply specify a clone name instead of relying on the plugin to generate it, that is supported as well:

 ```groovy
ephemeralName = 'testing_db'
 ```

 ```
❯ ./gradlew functionalTest

> Task :createEphemeral
Using credentials config file: /Users/stewartbryson/.snowflake/config.toml
Ephemeral clone testing_db created.

> Task :snowflakeJvm
Reusing existing connection.
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@upload/libs/java-testing-0.1.0-all.jar')


> Task :functionalTest

SnowflakeSampleTest

  Test ADD_NUMBERS() function with 1 and 2 PASSED
  Test ADD_NUMBERS() function with 3 and 4 PASSED

SUCCESS: Executed 2 tests in 3.3s


> Task :dropEphemeral
Reusing existing connection.
Ephemeral clone testing_db dropped.

BUILD SUCCESSFUL in 7s
13 actionable tasks: 4 executed, 9 up-to-date
```

# Autoconfiguration of `maven-publish` with External Stages
This option is useful when you want your artifacts available to consumers other than just Snowflake without publishing
them to disparate locations.
Gradle has [built-in support](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:s3-repositories)
for S3 or GCS as a Maven repository, and Snowflake has support for S3 or GCS external stages, so we simply marry the two
in a single location.
Looking at the [sample project](examples/java-external-stage/), notice we've populated a few additional properties:

```groovy
snowflake {
   connection = 'gradle_plugin'
   stage = 's3_maven'
   groupId = 'io.github.stewartbryson'
   artifactId = 'sample-udfs'
   applications {
      add_numbers {
         inputs = ["a integer", "b integer"]
         returns = "string"
         runtime = '17'
         handler = "Sample.addNum"
      }
   }
}
```

The `groupId` and `artifactId`, plus the built-in `version` property that exists for all Gradle builds, provide
the [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) for publishing externally to S3 or GCS.
I've also created a property in my local `gradle.properties` file for the bucket:

```properties
# local file in ~/.gradle/gradle.properties
snowflake.publishUrl='s3://myrepo/release'
```

The plugin doesn't create the stage, but it does do a check to ensure that the Snowflake stage metadata matches the
value in `publishUrl`. We get a few new tasks added to our project:

```
❯ ./gradlew tasks --group publishing

> Task :tasks

------------------------------------------------------------
Tasks runnable from root project 'java-external-stage'
------------------------------------------------------------

Publishing tasks
----------------
generateMetadataFileForSnowflakePublication - Generates the Gradle metadata file for publication 'snowflake'.
generatePomFileForSnowflakePublication - Generates the Maven POM file for publication 'snowflake'.
publish - Publishes all publications produced by this project.
publishAllPublicationsToS3_mavenRepository - Publishes all Maven publications produced by this project to the s3_maven repository.
publishSnowflakePublicationToMavenLocal - Publishes Maven publication 'snowflake' to the local Maven repository.
publishSnowflakePublicationToS3_mavenRepository - Publishes Maven publication 'snowflake' to Maven repository 's3_maven'.
publishToMavenLocal - Publishes all Maven publications produced by this project to the local Maven cache.
snowflakeJvm - A Cacheable Gradle task for publishing UDFs and procedures to Snowflake
snowflakePublish - Lifecycle task for all Snowflake publication tasks.

To see all tasks and more detail, run gradlew tasks --all

To see more detail about a task, run gradlew help --task <task>

BUILD SUCCESSFUL in 875ms
5 actionable tasks: 1 executed, 4 up-to-date
```

These are granular tasks for building metadata and POM files and publishing that along with the artifacts to S3.
But the `snowflakeJvm` task initiates all these dependent tasks,
including `publishSnowflakePublicationToS3_mavenRepository` which uploads the artifact but unfortunately doesn't provide
console output to that effect:

```
❯ ./gradlew snowflakeJvm --no-build-cache --console plain
> Task :gradle-snowflake:gradle-snowflake-plugin:compileJava NO-SOURCE
> Task :gradle-snowflake:gradle-snowflake-plugin:compileGroovy UP-TO-DATE
> Task :gradle-snowflake:gradle-snowflake-plugin:pluginDescriptors UP-TO-DATE
> Task :gradle-snowflake:gradle-snowflake-plugin:processResources UP-TO-DATE
> Task :gradle-snowflake:gradle-snowflake-plugin:classes UP-TO-DATE
> Task :gradle-snowflake:gradle-snowflake-plugin:jar UP-TO-DATE
> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :shadowJar
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :generatePomFileForSnowflakePublication
> Task :publishSnowflakePublicationToS3_mavenRepository

> Task :snowflakeJvm
Using credentials config file: /Users/stewartbryson/.snowflake/config.toml
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  runtime_version = '17'
  imports = ('@s3_maven/io/github/stewartbryson/sample-udfs/0.1.0/sample-udfs-0.1.0-all.jar')


BUILD SUCCESSFUL in 4s
9 actionable tasks: 5 executed, 4 up-to-date
```

# Manual configuration of `maven-publish` with External Stages

For organizations that already use `maven-publish` extensively, or have customizations outside the scope of
autoconfiguration, the plugin supports disabling autoconfiguration:

```groovy
useCustomMaven = true
```

We then configure `publications` and `repositories` as described in
the [`maven-publish` documentation](https://docs.gradle.org/current/userguide/publishing_maven.html), and
add [task dependencies](https://docs.gradle.org/current/userguide/publishing_maven.html) for the `snowflakeJvm` task.
The `publishUrl` property is no longer required because it's configured in the `publications` closure, but if provided,
the plugin will ensure it matches the metadata for the `stage` property.
`groupId` and `artifactId` are still required so that `snowflakeJvm` can autogenerate the `imports` section of
the `CREATE OR REPLACE...` statement.

# Contributing
Anyone can contribute! You don't need permission, my blessing, or expertise, clearly.
To make changes to the `README.md` file, please make them in the [master README file](src/markdown/README.md) instead.
The version tokens in this file are automatically replaced with the current value before publishing.

Three different testing tasks are defined:
```
❯ ./gradlew tasks --group verification

> Task :tasks

------------------------------------------------------------
Tasks runnable from root project 'gradle-snowflake'
------------------------------------------------------------

Verification tasks
------------------
check - Runs all checks.
functionalTest - Runs the functional test suite.
integrationTest - Runs the integration test suite.
test - Runs the test suite.

To see all tasks and more detail, run gradlew tasks --all

To see more detail about a task, run gradlew help --task <task>

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```

The `functionalTest` task contains all the tests that actually make a connection to Snowflake and test a deployment,
except those involved with external stages.
You need to add a connection in `~/.snowflake/config.toml` called `gradle_plugin`.
> WARNING: Ensure that the credentials you provide in `gradle_plugin` are safe for development purposes.

The `integrationTest` task requires the following external stages to exist in your Snowflake account:

* `gcs_maven`: An external stage in GCS.
* `s3_maven`: An external stage in S3.

It also requires the following gradle properties to be set, with the easiest method being placing them in `~/.gradle/gradle.properties`:
* `gcsPublishUrl`: the GCS url of `gcs_maven`.
* `s3PublishUrl`: the S3 url of `s3_maven`.

It is understandable if you are unable to test external stages as part of your contribution.
We segmented them out for this reason.

Open a pull request against the `develop` branch so that it can be merged and possibly tweaked before 
we open the PR against the `main` branch.
This will also enable us to test external stages.