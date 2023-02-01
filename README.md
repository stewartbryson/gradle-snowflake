# Breaking Changes
I've introduced breaking changes in version `1.0.0`.
In preparation for supporting non-JVM languages, the task `snowflakePublish` has been renamed to `snowflakeJvm`.
`snowflakeJvm` will be the task for deploying all JVM-based languages, including Java, Groovy, Scala and any others that may be supported by Gradle through plugins.

I created a [lifecycle task](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:lifecycle_tasks) called `snowflakePublish` that depends on `snowflakeJvm`, which should eliminate many build script changes.
However, the `snowflakeJvm` task (previously `snowflakePublish`) does have command-line options.
In case you are using those options, then you will have to make build script changes.
This documentation has been updated to reflect these changes.

# Motivation
It needs to be easy to develop and test JVM applications even if they are being deployed to Snowflake using Snowpark and UDFs.
Using [Gradle](https://www.gradle.org), we can easily build shaded JAR files with dependencies using the [shadow plugin](https://imperceptiblethoughts.com/shadow/), and I've provided a [sample Java project](examples/simple-jar/) that demonstrates this basic use case:

```
cd examples/simple-jar
./gradlew build
```

But this JAR would still have to be uploaded to a stage in Snowflake, and the UDF would have to be created or possibly recreated if its signature changed.
I wanted an experience using Snowflake that is as natural to developers using IntelliJ or VS Code for standard Java projects.

# The Snowflake Plugin
This plugin provides easy configuration options for those getting started with Gradle but also provides advanced features for teams already using Gradle in other areas of the organization.
It has three basic modes:

1. Lightweight publishing to internal Snowflake stages using Snowpark.
2. Slightly heavier publishing using external Snowflake stages and auto-configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.
3. Publishing to Snowflake using external stages and custom configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

Have a look at the [API docs](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/package-summary.html).

This plugin can be used to build UDFs in any JVM language supported by Gradle, which currently provides official support for Java, Scala, Kotlin and Groovy.
See the [examples](examples) directory for examples using different languages.

# Internal Stages using Snowpark
Unless you have a heavy investment in Gradle as an organization, this is likely the option you want to use.
Additionally, if you plan on *sharing* UDFs across Snowflake accounts, this is the option you *have* to use, as JARs need to be in named internal stages.
Look at the [sample project](examples/internal-stage/) and you'll notice a few differences in the [build file](examples/internal-stage/build.gradle).
We applied `io.github.stewartbryson.snowflake` and removed `com.github.johnrengelman.shadow` because the `shadow` plugin is automatically applied by the `snowflake` plugin:

```groovy
plugins {
    id 'java'
    id 'com.github.ben-manes.versions' version '0.42.0'
    id 'io.github.stewartbryson.snowflake' version '1.0.14'
}
```

We now have the `snowflakeJvm` task available:

```
❯ ./gradlew help --task snowflakeJvm

> Task :help
Detailed task information for SnowflakeJvm

Path
     :snowflakeJvm

Type
     SnowflakeJvm (io.github.stewartbryson.SnowflakeJvm)

Options
     --account     Override the URL of the Snowflake account.

     --database     Override the Snowflake database to connect to.

     --ephemeral-name     Optional: specify the ephemeral database name instead of relying on an autogenerated value.

     --jar     Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.

     --keep-ephemeral     When enabled, don't drop the ephemeral Snowflake database clone.

     --password     Override the Snowflake password to connect with.

     --role     Override the Snowflake role to connect with.

     --schema     Override the Snowflake schema to connect with.

     --stage     Override the Snowflake stage to publish to.

     --use-ephemeral     When enabled, run using an ephemeral Snowflake database clone.

     --user     Override the Snowflake user to connect as.

     --warehouse     Override the Snowflake warehouse to use.

Description
     A Cacheable Gradle task for publishing UDFs to Snowflake.

Group
     publishing
:help (Thread[included builds,5,main]) completed. Took 0.027 secs.

BUILD SUCCESSFUL in 3s
```

Several command-line options mention _overriding_ other configuration values.
This is because the plugin also provides a configuration closure called `snowflake` that we can use to configure our build, all of which are documented in the [class API](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/SnowflakeExtension.html):

```groovy
snowflake {
    // All the following options are provided in my local gradle.properties file
    // account = <snowflake account url>
    // user = <snowflake user>
    // password = <snowflake password>
    role = 'stewart_role'
    database = 'stewart_db'
    schema = 'developer'
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

Notice that I'm not hard-coding sensitive credentials.
Instead, they are in my local `gradle.properties` file, and any of the plugin configs can be provided this way or [other ways](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) using Gradle project properties:

```properties
# local file in ~/.gradle/gradle.properties
snowflake.account = https://myorg.snowflakecomputing.com:443
snowflake.user = myusername
snowflake.password = mypassword
```

The nested [`applications` closure](https://s3.amazonaws.com/stewartbryson.docs/gradle-snowflake/latest/io/github/stewartbryson/ApplicationContainer.html) might seem a bit more daunting.
This is a simple way to use DSL to configure all the different UDFs we want to automatically create (or recreate) each time we publish the JAR file.
The example above will generate and execute the statement:

```roomsql
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')
```

With our configuration complete, we can execute the `snowflakeJvm` task, which will run any unit tests and then publish our JAR and create our function.
Note that if the named internal stage does not exist, the plugin will create it first:

```
❯ ./gradlew snowflakeJvm

> Task :snowflakeJvm
File internal-stage-0.1.0-all.jar: UPLOADED
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')


BUILD SUCCESSFUL in 10s
3 actionable tasks: 3 executed
```

Our function now exists in Snowflake:

```roomsql
select add_numbers(1,2);
```

The `snowflakeJvm` task was written to be [*incremental*](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:up_to_date_checks) and [*cacheable*](https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching_details).
If we run the task again without making any changes to task inputs (our code) or outputs, then the execution is avoided, which we know because of the *up-to-date* keyword.

```
❯ ./gradlew snowflakeJvm

BUILD SUCCESSFUL in 624ms
3 actionable tasks: 3 up-to-date
```

# Auto-configuration of `maven-publish` with External Stages
This option is useful when you want your artifacts available to consumers other than just Snowflake without publishing them to disparate locations.
Gradle has [built-in support](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:s3-repositories) for S3 or GCS as a Maven repository, and Snowflake has support for S3 or GCS external stages, so we simply marry the two in a single location.
Looking at the [sample project](examples/external-stage/), notice we've populated a few additional properties:

```groovy
snowflake {
    // All the following options are provided in my local gradle.properties file
    // account = <snowflake account url>
    // user = <snowflake user>
    // password = <snowflake password>
    // publishUrl = <S3 bucket and path>
    role = 'stewart_role'
    database = 'stewart_db'
    schema = 'developer'
    stage = 'maven'
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

The `groupId` and `artifactId`, plus the built-in `version` property that exists for all Gradle builds, provide the [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) for publishing externally to S3 or GCS.
I've also created a property in my local `gradle.properties` file for the bucket:

```properties
# local file in ~/.gradle/gradle.properties
snowflake.publishUrl = 's3://myrepo/release'
```

The plugin doesn't create the stage, but it does do a check to ensure that the Snowflake stage metadata matches the value in `publishUrl`. We get a few new tasks added to our project:

```
❯ ./gradlew tasks --group publishing

> Task :tasks

------------------------------------------------------------
Tasks runnable from root project 'unit-test'
------------------------------------------------------------

Publishing tasks
----------------
generateMetadataFileForSnowflakePublication - Generates the Gradle metadata file for publication 'snowflake'.
generatePomFileForSnowflakePublication - Generates the Maven POM file for publication 'snowflake'.
publish - Publishes all publications produced by this project.
publishAllPublicationsToMavenRepository - Publishes all Maven publications produced by this project to the maven repository.
publishSnowflakePublicationToMavenLocal - Publishes Maven publication 'snowflake' to the local Maven repository.
publishSnowflakePublicationToMavenRepository - Publishes Maven publication 'snowflake' to Maven repository 'maven'.
publishToMavenLocal - Publishes all Maven publications produced by this project to the local Maven cache.
snowflakeJvm - A Cacheable Gradle task for publishing UDFs to Snowflake.
snowflakePublish - Lifecycle task for all Snowflake publication tasks.

To see all tasks and more detail, run gradle tasks --all

To see more detail about a task, run gradle help --task <task>

BUILD SUCCESSFUL in 4s
1 actionable task: 1 executed
```

These are granular tasks for building metadata and POM files and publishing that along with the artifacts to S3.
But the `snowflakeJvm` task initiates all these dependent tasks, including `publishSnowflakePublicationToMavenRepository` which uploads the artifact but unfortunately doesn't provide console output to that effect:

```
❯ ./gradlew snowflakeJvm --console=plain
> Task :compileJava
> Task :processResources NO-SOURCE
> Task :classes
> Task :shadowJar
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :generatePomFileForSnowflakePublication
> Task :publishSnowflakePublicationToMavenRepository

> Task :snowflakeJvm
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@maven/io/github/stewartbryson/sample-udfs/0.1.0/sample-udfs-0.1.0-all.jar')


BUILD SUCCESSFUL in 12s
5 actionable tasks: 5 executed
```

 # Manual configuration of `maven-publish` with External Stages
For organizations that already use `maven-publish` extensively, or have customizations outside the scope of auto-configuration, the plugin supports disabling auto-configuration:

```groovy
useCustomMaven = true
```


We then configure `publications` and `repositories` as described in the [`maven-publish` documentation](https://docs.gradle.org/current/userguide/publishing_maven.html), and add [task dependencies](https://docs.gradle.org/current/userguide/publishing_maven.html) for the `snowflakeJvm` task.
The `publishUrl` property is no longer required because it's configured in the `publications` closure, but if provided, the plugin will ensure it matches the metadata for the `stage` property.
`groupId` and `artifactId` are still required so that `snowflakeJvm` can autogenerate the `imports` section of the `CREATE OR REPLACE...` statement.

# Testing deployments with ephemeral databases
Running unit tests using static Snowflake databases is boring, especially considering the [zero-copy cloning](https://docs.snowflake.com/en/user-guide/object-clone.html#cloning-considerations) functionality available.
The `snowflakeJvm` task supports cloning an ephemeral database from the database we connect to and publishing to the clone instead.
This workflow is useful for CI/CD processes testing pull requests and is accessible either through the configuration closure, or as an option passed directly to the Gradle task.
To demonstrate, we'll use the `internal-stage` project referenced above.
We can do either of the following:

```groovy
useEphemeral = true
```
or

```
❯ ./gradlew snowflakeJvm --use-ephemeral                 

> Task :snowflakeJvm
Ephemeral clone ephemeral_internalstage_yyuG7AcRF created.
File internal-stage-0.1.0-all.jar: UPLOADED
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA 
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')

Ephemeral clone ephemeral_internalstage_yyuG7AcRF dropped.

BUILD SUCCESSFUL in 19s
3 actionable tasks: 1 executed, 2 up-to-date
```

The plugin is aware when it is running in CI/CD environments and currently supports:
 * [Travis CI](https://travis-ci.org)
 * [Jenkins](https://jenkins.io)
 * [GitLab CI](https://about.gitlab.com/product/continuous-integration/)
 * [GitHub Actions](https://github.com/features/actions)
 * [Appveyor](https://www.appveyor.com)

 When the CI/CD environment is detected, the plugin will name the ephemeral database clone based on the pull request number, the branch name, or the tag name instead of the auto-generated name shown above:

 ```
 ❯ ./gradlew snowflakeJvm --use-ephemeral

> Task :snowflakeJvm
Ephemeral clone ephemeral_internalstage_pr_46 created.
File internal-stage-0.1.0-all.jar: UPLOADED
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA 
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')

Ephemeral clone ephemeral_internalstage_pr_46 dropped.

BUILD SUCCESSFUL in 29s
 ```

 If we prefer to simply specify a clone name instead of relying on the plugin to generate it, that is supported as well:

 ```groovy
 useEphemeral = true
 ephemeralName = 'testing_db'
 ```
 or
 ```
 ❯ ./gradlew snowflakeJvm --use-ephemeral --ephemeral-name testing_db

> Task :snowflakeJvm
Ephemeral clone testing_db created.
File internal-stage-0.1.0-all.jar: UPLOADED
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA 
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')

Ephemeral clone testing_db dropped.

BUILD SUCCESSFUL in 41s
3 actionable tasks: 1 executed, 2 up-to-date
```

Finally, if you want to keep the ephemeral database after the build is complete, simply pass the `--keep-ephemeral` option and it won't be dropped.
This is useful for manual prototyping to ensure our applications are being deployed successfully, but shouldn't be used for automated CI/CD workflows unless you want to create the clone when a pull request is opened and drop it when it is closed:

```
❯ ./gradlew snowflakeJvm --use-ephemeral --keep-ephemeral           

> Task :snowflakeJvm
Ephemeral clone ephemeral_internalstage_r0sf0U1ix created.
File internal-stage-0.1.0-all.jar: UPLOADED
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA 
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')


BUILD SUCCESSFUL in 35s
3 actionable tasks: 1 executed, 2 up-to-date
```
# Contributing
To make changes to the `README.md` file, please make them in the [master README file](src/markdown/README.md) instead.
The version tokens in this file are automatically replaced with the current value before publishing.

Two different unit test tasks are defined:
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
test - Runs the test suite.

To see all tasks and more detail, run gradle tasks --all

To see more detail about a task, run gradle help --task <task>

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```
The `functionalTest` task contains all the tests that actually make a connection to Snowflake and test a deployment.
> WARNING: Ensure that the credential you provide below are for a safe development database.

To run `functionalTest`, create the following entries in `~/.gradle/gradle.properties`:
```properties
snowflake.account=https://myaccount.snowflakecomputing.com:443
snowflake.user=myuser
snowflake.password=mypassword
snowflake.database=mydatabase
snowflake.role=myrole
snowflake.schema=myschema
snowflake.stage=mystage
snowflake.warehouse=compute_wh
```

Open a pull request against the `main` branch.
The Action that tests the PR won't run until I've reviewed it.
