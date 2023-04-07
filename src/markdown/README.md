# Breaking Changes

We introduced breaking changes in version `2.0.0` described below.

### SnowSQL Config File
Instead of continuing to use plugin DSL or Gradle properties to provide Snowflake authentication, we made the
decision to switch to using the SnowSQL config moving forward.
This was inspired by the [Snowflake Developer CLI](https://github.com/Snowflake-Labs/snowcli) project, and it seems to
be a reasonable standard.

### Legacy Plugin Application
Hopefully no one was using the Gradle legacy plugin application, but if so, the coordinates have changed.
You can always get the most recent coordinates on
the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.stewartbryson.snowflake).

### Ephemeral Cloning for Functional Testing
Initially, the ephemeral cloning functionality was only present for the `snowflakeJvm` task, with the clone being
created (and possibly dropped) as part of that task.
To support the new functional testing feature (described below), ephemeral cloning was moved out into the `createEphemeral`
and `dropEphemeral` tasks that are now added at the beginning and end of the build, respectively.
This allows for the `functionalTest` task (if applied) to be run in the ephemeral clone just after `snowflakeJvm`.
As described below, cloning tasks are automatically managed with the `useEphemeral`, `keepEphemeral`
and `ephemeralName` properties as before, but the task options on `snowflakeJvm` have been removed.

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
I wanted an experience using Snowflake that is as natural to developers using IntelliJ or VS Code for standard Java
projects.

# The Snowflake Plugin

This plugin provides easy configuration options for those getting started with Gradle but also provides advanced
features for teams already using Gradle in other areas of the organization.
It has three basic modes:

1. Lightweight publishing to internal Snowflake stages using Snowpark.
2. Slightly heavier publishing using external Snowflake stages and auto-configuration of
   the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.
3. Publishing to Snowflake using external stages and custom configuration of
   the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

Have a look at
the [API docs](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/package-summary.html)
.

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
   id 'io.github.stewartbryson.snowflake' version '@version@'
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
     --connection     Override the SnowSQL connection to use. Default: use the base connection info in SnowSQL config.

     --jar     Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.

     --snow-config     Custom SnowSQL config file.

     --stage     Override the Snowflake stage to publish to.

     --rerun     Causes the task to be re-run even if up-to-date.

Description
     A Cacheable Gradle task for publishing UDFs and procedures to Snowflake.

Group
     publishing

BUILD SUCCESSFUL in 1s
5 actionable tasks: 1 executed, 4 up-to-date
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
         handler = "Sample.addNum"
      }
   }
}
```

Beginning in version `2.0.0`, the plugin uses
the [SnowSQL config](https://docs.snowflake.com/en/user-guide/snowsql-config) file, a choice that was inspired by
the [Snowflake Developer CLI](https://github.com/Snowflake-Labs/snowcli) project.
The `connection` property in the plugin DSL defines which connection from the config file to use, relying on the default values if none is
provided.
It first loads all the default values, and replaces any values from the connection, similar to how SnowSQL works.
Unfortunately, it doesn't yet look for a config file in all the places that SnowSQL does.
Instead, it looks in this order:

1. `<HOME_DIR>/.snowsql/config`
2. `./snow-config` (Useful in CI/CD pipelines, where secrets can be easily written to this file.)
3. A custom location of your choosing, configured with the `--snow-config` option in applicable tasks.

The nested 
[`applications` DSL](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/ApplicationContainer.html)
might seem a bit daunting.
This is a simple way to configure all the different UDFs or procedures we want to automatically create (or recreate)
each time we publish the JAR file.
The example above will generate and execute the following statement.
Notice that the `imports` part of the statement is automatically added by the plugin as the JAR (including our code and all dependencies) is automatically uploaded to Snowflake:

```roomsql
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/java-0.1.0-all.jar')
```

With the configuration complete, we can execute the `snowflakeJvm` task, which will run any unit tests (see _Testing_
below) and then publish
our JAR and create our function.
Note that if the named internal stage does not exist, the plugin will create it first:

```
❯ ./gradlew snowflakeJvm

> Task :snowflakeJvm
Using snowsql config file: /Users/stewartbryson/.snowsql/config
File java-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/java-0.1.0-all.jar')


BUILD SUCCESSFUL in 4s
7 actionable tasks: 7 executed
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
    id 'io.github.stewartbryson.snowflake' version '@version@'
}
```

Our choice to use the Spock framework in the default `test` testing suite:
```groovy
test {
   useSpock('2.3-groovy-3.0')
}
```

And the unit test `SampleTest` spec:
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
      def one = 1
      def two = 2
      then: "Add numbers"
      sample.addNum(one, two) == "Sum is: 3"
   }

   def "adding 3 and 4"() {
      when: "Two numbers"
      def one = 3
      def two = 4
      then: "Add numbers"
      sample.addNum(one, two) == "Sum is: 7"
   }
}
```

All unit tests in either `src/test/java` (written using JUnit or something else) or `src/test/groovy` (written with Spock)
we automatically run whenever the `test` or `build` task is executed.

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
The same applies for topic of functional testing below.

### Functional Testing
[Functional testing](https://en.wikipedia.org/wiki/Functional_testing) describes what the system does,
and in my mind, this involves testing our deployed code in Snowflake. 
Regardless of what we call it, we _know we need this_, and it's a crucial component in our build chain.
This plugin contains a custom Spock Specification class called `SnowflakeSpec` that can be used in a new test suite.
By default, this test suite is called `functionalTest`, though the name can be configured using the `testSuite` property.

Here is our configuration of the `functionalTest` test suite. It's convoluted DSL (in my opinion), but no one asked me:
```grovy
functionalTest(JvmTestSuite) {
   targets {
       all {
           useSpock('2.3-groovy-3.0')
           dependencies {
               implementation "io.github.stewartbryson:gradle-snowflake-plugin:@version@"
           }
           testTask.configure {
               failFast true
               // which SnowSQL connection to use
               systemProperty 'connection', project.snowflake.connection
           }
       }
   }
}
```

I'll walk through a few of these points.
So that the `SnowflakeSpec` is available in the test classpath, we have to declare the plugin as a dependency
to the test suite.
Notice that we use the library standard maven coordinates, which are different than the coordinates in the `plugin` DSL.
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
        def one = 1
        def two = 2

        then: 'Add two numbers using ADD_NUMBERS()'
        selectSingleValue("select add_numbers($one,$two);") == 'Sum is: 3'
    }

    def 'ADD_NUMBERS() function with 3 and 4'() {
        when: "Two numbers exist"
        def three = 3
        def four = 4

        then: 'Add two numbers using ADD_NUMBERS()'
        selectSingleValue("select add_numbers($three,$four);") == 'Sum is: 7'
    }

}
```
The `selectSingleValue` method returns the first column from the first row in a `SELECT` statement,
so it's perfect for testing a function. And of course, this executes against Snowflake in real time.

```shell
❯ ./gradlew functionalTest

> Task :test

SampleTest

  Test adding 1 and 2 PASSED
  Test adding 3 and 4 PASSED

SUCCESS: Executed 2 tests in 462ms


> Task :snowflakeJvm
Using snowsql config file: /Users/stewartbryson/.snowsql/config
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/java-testing-0.1.0-all.jar')


> Task :functionalTest

SnowflakeSampleTest

  Test ADD_NUMBERS() function with 1 and 2 PASSED
  Test ADD_NUMBERS() function with 3 and 4 PASSED

SUCCESS: Executed 2 tests in 3.4s


BUILD SUCCESSFUL in 9s
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
         handler = "Sample.addNum"
      }
   }
}
```

The `useEphemeral` property will determine whether the `createEphemeral` and `dropEphemeral` tasks 
are added at the beginning and end of the build, respectively.
This allows for the `functionalTest` task to be run in the ephemeral clone just after our application is published.
We've also added a little extra magic to keep the clone when building a pull request.
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
               // which SnowSQL connection to use
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
Using snowsql config file: /Users/stewartbryson/.snowsql/config
Ephemeral clone EPHEMERAL_JAVA_TESTING_JYLM4SB2W created.

> Task :snowflakeJvm
Reusing existing connection.
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/java-testing-0.1.0-all.jar')


> Task :functionalTest

SnowflakeSampleTest

  Test ADD_NUMBERS() function with 1 and 2 PASSED
  Test ADD_NUMBERS() function with 3 and 4 PASSED

SUCCESS: Executed 2 tests in 3s


> Task :dropEphemeral
Reusing existing connection.
Ephemeral clone EPHEMERAL_JAVA_TESTING_JYLM4SB2W dropped.

BUILD SUCCESSFUL in 6s
13 actionable tasks: 4 executed, 9 up-to-date
```

When the CI/CD environment is detected, the plugin will name the ephemeral database clone based on the pull request
number, the branch name, or the tag name instead of the auto-generated name based on Gradle project.
If we prefer to simply specify a clone name instead of relying on the plugin to generate it, that is supported as well:

 ```groovy
ephemeralName = 'testing_db'
 ```

 ```
❯ ./gradlew functionalTest

> Task :createEphemeral
Using snowsql config file: /Users/stewartbryson/.snowsql/config
Ephemeral clone testing_db created.

> Task :snowflakeJvm
Reusing existing connection.
File java-testing-0.1.0-all.jar: UPLOADED
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
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

# Auto-configuration of `maven-publish` with External Stages
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
❯ gradle snowflakeJvm --no-build-cache --console plain
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
Using snowsql config file: /Users/stewartbryson/.snowsql/config
Deploying ==>
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
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
Anyone can contribute!
To make changes to the `README.md` file, please make them in the [master README file](src/markdown/README.md) instead.
The version tokens in this file are automatically replaced with the current value before publishing.

Three different unit test tasks are defined:
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
You need to add a connection in `~/.snowsql/config` called `gradle_plugin`.
> WARNING: Ensure that the credential you provide below are for a safe development database.

The `integrationTest` requires the following external stages to exist in your Snowflake account:

* `gcs_maven`: An external stage in GCS.
* `s3_maven`: An external stage in S3.

It is understandable if you are unable to test external stages as part of your contribution.
I segmented them out for this reason.

Open a pull request against the `develop` branch so that it can be merged and possibly tweaked before I open the PR against the `main` branch.
This will also enable me to test external stages.