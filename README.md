# Motivation
It needs to be easy to develop, test and deploy Java and Scala applications, even if they are being deployed to Snowflake using Snowpark and UDFs.
Using [Apache Gradle](www.gradle.org), we can easily build shaded JAR files with dependencies using the [shadow plugin](https://imperceptiblethoughts.com/shadow/), and I've provided a [sample project](examples/simple-jar/) that demonstrates this basic use case:

```
cd examples/simple-jar
./gradlew build
```

But this JAR would still have to be uploaded to a stage in Snowflake, and the UDF would have to be created or possibly recreated if it's signature changed.

I wanted an easy test and deploy framework that was as natural to developers in IntelliJ as any other deployment target.

# The gradle-snowflake Plugin
This plugin provides easy configuration options for those getting started using Gradle to build software, but also provides advanced features for teams already using Gradle in other areas of the organization.
It has three basic modes:

1. Lightweight publishing to internal Snowflake stages using Snowpark.
2. Slightly heavier publishing using external Snowflake stages and auto-configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.
3. Publishing to Snowflake using external stages and custom configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

Have a look at the [API docs](https://s3.amazonaws.com/docs.noumenal.io/gradle-snowflake/latest/io/noumenal/package-summary.html).

# Internal Stages using Snowpark
Unless you have a heavy investment in Gradle as an organization, this is likely the option you want to use.
Additionally, if you plan on *sharing* UDFs across Snowflake accounts, this is the option you *have* to use, as JARs need to be in named internal stages.
Look at the [sample project](examples/internal-stage/) and you'll notice a few differences in the [build file](examples/internal-stage/build.gradle). We have applied the `io.noumenal.gradle.snowflake` plugin, and we are no longer applying the `com.github.johnrengelman.shadow` plugin:

```
plugins {
    id 'java'
    id 'com.github.ben-manes.versions' version '0.42.0'
    id 'io.noumenal.gradle.snowflake' version '0.1.11'
}
```

When the plugin is added, we now get a new task in our Gradle project:

```
❯ ./gradlew help --task snowflakePublish

> Task :help
Detailed task information for snowflakePublish

Path
     :snowflakePublish

Type
     SnowflakePublish (io.noumenal.SnowflakePublish)

Options
     --account     Override the URL of the Snowflake account.

     --database     Override the Snowflake database to connect to.

     --jar     Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.

     --password     Override the Snowflake password to connect with.

     --role     The Snowflake role to use.

     --schema     Override the Snowflake schema to connect with.

     --stage     The Snowflake external stage to publish to.

     --user     Override the Snowflake user to connect as.

     --warehouse     Override the Snowflake role to connect with.

Description
     Publish a Java artifact to an external stage and create Snowflake Functions and Procedures.

Group
     publishing

BUILD SUCCESSFUL in 597ms
1 actionable task: 1 executed
```

There are a number of command-line options that mention *overriding* other configuration values.
This is because the plugin also provides a configuration closure called `snowflake` that we can now use to set all these values in our build file.
The specific options for this closure are documented in the [API docs](https://s3.amazonaws.com/docs.noumenal.io/gradle-snowflake/latest/io/noumenal/SnowflakeExtension.html):

```
snowflake {
    // All the following options are provided in my local gradle.properties file
    // url = <snowflake account url>
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
            handler = "AddNumbers.addNum"
        }
    }
}
```

Notice that I'm not hard-coding sensitive credentials.
Instead, they are in my local `gradle.properties` file, and any of the plugin configs can be provided this way, or any [number of ways](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) using Gradle project properties:

```
snowflake.url = https://myorg.snowflakecomputing.com:443
snowflake.user = myusername
snowflake.password = mypassword
```

The nested `applications` closure might seem a bit more daunting.
This is a simple way to use DSL to configure all the different UDFs we want to automatically create (or recreate) each time we publish the JAR file.
This example will generate the command:

```
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@upload/libs/internal-stage-0.1.0-all.jar')
```

With our configuration complete, we can execute the `snowflakePublish` command, which will run any unit tests and then publish our JAR and create our function:

```
❯ ./gradlew snowflakePublish --rerun-tasks

> Task :snowflakePublish
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

```
select add_numbers(1,2);
```

The `snowflakePublish` task was also written to be [incremental](https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:up_to_date_checks) and [*cacheable*](https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching_details).
If I run the task again without making any changes to my code, then the execution is avoided, which we know because of the *up-to-date* keyworld.

```
❯ ./gradlew snowflakePublish              

BUILD SUCCESSFUL in 624ms
3 actionable tasks: 3 up-to-date
```

# Auto-configuration of `maven-plugin` with External Stages
This option is useful when you want to make your artifacts availablle to more consumers than just Snowflake and you aren't interested in publishing them to a bunch of disparate locations.
Gradle has [built-in support](https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:s3-repositories) for S3 as a Maven repository, and Snowflake has support for S3 external stages.
If you look at the [sample project](examples/external-stage/), you will notice we've populated a few additional properties:

```
groupId = 'io.noumenal'
artifactId = 'sample-udfs'
```

These, plus the built-in `version` property that exists for all Gradle builds, provide the [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) for publishing externally to S3.
I've also created a property in my local `gradle.properties` file for the bucket:

```
snowflake.publishUrl = 's3://myrepo/release'
```

The plugin doesn't create the stage, but it does do a check to ensure that the Snowflake stage metadata matches the value in `publishUrl`. We get a few new tasks added to our project:

```
❯ gradle tasks --group publishing

> Task :tasks

------------------------------------------------------------
Tasks runnable from root project 'external-stage'
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
snowflakePublish - Publish a Java artifact to an external stage and create Snowflake Functions and Procedures.

To see all tasks and more detail, run gradle tasks --all

To see more detail about a task, run gradle help --task <task>

BUILD SUCCESSFUL in 600ms
1 actionable task: 1 executed
```

These are a bunch of granular tasks for building metadata and POM files, and publishing that along with the artifacts to S3.
But the `snowflakePublish` task manages initating all these dependent tasks, including `publishSnowflakePublicationToMavenRepository` which actually uploads the artifact:

```
❯ gradle snowflakePublish --console=plain
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

> Task :snowflakePublish
Deploying ==> 
CREATE OR REPLACE function add_numbers (a integer, b integer)
  returns string
  language JAVA
  handler = 'Sample.addNum'
  imports = ('@maven/io/noumenal/sample-udfs/0.1.0/sample-udfs-0.1.0-all.jar')


BUILD SUCCESSFUL in 12s
5 actionable tasks: 5 executed
```

 # Manual configuration of `maven-plugin` with External Stages
This use case is for organizations that already use the `maven-publish` plugin extensively, and prefer to do all the manual configuration required to use it, or have customizations that are outside the scope of auto-configuration.
In this case, we have to configure `publications` and `repositories` as described in the [`maven-publish` documentation](https://docs.gradle.org/current/userguide/publishing_maven.html), and add [task dependencies](https://docs.gradle.org/current/userguide/publishing_maven.html) for the `snowflakePublish` task.
We no longer have to provide a `publishUrl` to the plugin because we are configuring that location ourselves now, but we still have to provide `artifactId` and `groupId` so that `snowflakePublish` can correctly autogenerate the `imports` portion of the `CREATE OR REPLACE` command.