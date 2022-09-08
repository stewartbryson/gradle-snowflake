# Motivation
It needs to be easy to develop, test and deploy Java and Scala applications, even if they are being deployed to Snowflake using Snowpark and UDFs.
Using [Apache Gradle](www.gradle.org), we can easily build shaded JAR files with dependencies using the [shadow plugin](https://imperceptiblethoughts.com/shadow/), and I've provided a [sample project](examples/simple-jar/) that demonstrates this basic use case:

```
cd examples/simple-jar
./gradlew shadowJar
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

The plugin provides a configuration closure called `snowflake` that we can now use.
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

The first thing you'll notice is that I'm not hard-coding the sensitive credentials.
Instead, they are in my local `gradle.properties` file, and it's worth mentioning that any of the plugin configs can be provided this way, or any [number of ways](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) using Gradle project properties:

```
snowflake.url = https://my-org.snowflakecomputing.com:443
snowflake.user = myusername
```

The nested `applications` closure might seem a bit more daunting.
This is simply a way to configure using DSL all the different UDFs we want to automatically create (or recreate) each time we publish the JAR file.
This example will generate the command:

```
