package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * A simple functional test for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class ScalaTest extends Specification {
    @Shared
    def result

    @Shared
    String taskName

    @TempDir
    @Shared
    private File projectDir

    @Shared
    File buildFile, settingsFile, scalaFile

    @Shared
    String ephemeralName = 'ephemeral_unit_test'

    @Shared
    String account = System.getProperty("snowflake.account"),
           user = System.getProperty("snowflake.user"),
           password = System.getProperty("snowflake.password"),
           s3PublishUrl = System.getProperty("snowflake.s3PublishUrl"),
           gcsPublishUrl = System.getProperty("snowflake.gcsPublishUrl"),
           role = System.getProperty("snowflake.role"),
           database = System.getProperty("snowflake.database"),
           schema = System.getProperty("snowflake.schema"),
           internalStage = System.getProperty("internalStage"),
           s3Stage = System.getProperty("s3Stage"),
           gcsStage = System.getProperty("gcsStage")

    def setupSpec() {
        settingsFile = new File(projectDir, 'settings.gradle')
        settingsFile.write("""
                     |rootProject.name = 'unit-test'
                     |""".stripMargin())

        buildFile = new File(projectDir, 'build.gradle')
        buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id 'scala'
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |repositories {
                    |    mavenCentral()
                    |}
                    |dependencies {
                    |    implementation 'org.scala-lang:scala-library:2.13.10'
                    |}
                    |snowflake {
                    |  role = '$role'
                    |  database = '$database'
                    |  schema = '$schema'
                    |  applications {
                    |      add_numbers {
                    |         inputs = ["a integer", "b integer"]
                    |         returns = "string"
                    |         handler = "Sample.addNum"
                    |      }
                    |   }
                    |}
                    |version='0.1.0'
                    |""".stripMargin())

        scalaFile = new File("${projectDir}/src/main/scala", 'Sample.scala')
        scalaFile.parentFile.mkdirs()
        scalaFile.write("""|
                            |class Sample {
                            |  def addNum(num1: Integer, num2: Integer): String = {
                            |    try {
                            |      "The sum is: " + (num1 + num2).toString()
                            |    } catch {
                            |      case e: Exception => return null
                            |    }
                            |  }
                            |}
                  |""".stripMargin())
    }

    // helper method
    def executeSingleTask(String taskName, List args, Boolean logOutput = true) {
        // ultra secure handling
        List systemArgs = [
                "-Psnowflake.account=$account".toString(),
                "-Psnowflake.user=$user".toString(),
                "-Psnowflake.password=$password".toString()
        ]
        args.add(0, taskName)
        args.addAll(systemArgs)

        // execute the Gradle test build
        result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
                .build()

        // log the results
        if (logOutput) log.warn result.getOutput()
        return result
    }

    def "shadowJar"() {
        given:
        taskName = 'shadowJar'

        when:
        result = executeSingleTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm for Scala"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", internalStage, '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm for Scala with ephemeral"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", internalStage, "--use-ephemeral", '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(dropped)(.+)/)
    }
}
