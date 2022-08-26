# Motivation
It needs to be easy to develop, test and deploy Java and Scala applications, even if they are being deployed to Snowflake using Snowpark and UDFs.
Using [Apache Gradle](www.gradle.org), we can easily build thick JAR files with dependencies, but the deployment to Snowflake still felt very manual.
I wanted an easy test and deploy framework that was as natural to developers in IntelliJ as any other deployment target.

# The gradle-snowflake Plugin
This plugin provides easy configuration options for those getting started using Gradle to build software, but also provides advanced features for teams already using Gradle in other areas of the organization.
It has three basic modes:

1. Lightweight publishing to internal Snowflake stages using Snowpark.
2. Slightly heavier publishing using external Snowflake stages and auto-configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.
3. Publishing to Snowflake using external stages and custom configuration of the [`maven-publish`](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

