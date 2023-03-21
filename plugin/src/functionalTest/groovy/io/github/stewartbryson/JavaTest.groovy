package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class JavaTest extends Specification {
    @Shared
    def result

    @Shared
    String taskName

    @TempDir
    @Shared
    private File projectDir

    @Shared
    File buildFile, settingsFile, javaFile

    @Shared
    String ephemeralName = 'ephemeral_unit_test'

    @Shared
    String account = System.getProperty("snowflake.account"),
           warehouse = System.getProperty("snowflake.warehouse"),
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
                    |    id 'java'
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |snowflake {
                    |  groupId = 'io.github.stewartbryson'
                    |  artifactId = 'test-gradle-snowflake'
                    |  role = '$role'
                    |  database = '$database'
                    |  schema = '$schema'
                    |  warehouse = '$warehouse'
                    |  ephemeralName = '$ephemeralName'
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

        javaFile = new File("${projectDir}/src/main/java", 'Sample.java')
        javaFile.parentFile.mkdirs()
        javaFile.write("""
                  |public class Sample
                  |{
                  |  public String addNum(int num1, int num2) {
                  |    try {
                  |      int sum = num1 + num2;
                  |      return ("Sum is: " + sum);
                  |    } catch (Exception e) {
                  |      return e.toString();
                  |      }
                  |    }
                  |
                  |    public static void main(String[] args){
                  |      System.out.println("Hello World");
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

        // Don't print the password
        //log.warn "runner arguments: ${args}"

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

    def "snowflakeJvm with S3 publishUrl option"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", s3Stage, "-Psnowflake.publishUrl=$s3PublishUrl".toString(), '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm with GCS publishUrl option"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", gcsStage, "-Psnowflake.publishUrl=$gcsPublishUrl".toString(), '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm without publishUrl option"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", internalStage, '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm with custom JAR"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ['--jar', 'build/libs/unit-test-0.1.0-all.jar', '--stage', 'upload', '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm with immutable function"() {
        given:
        buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id 'java'
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |snowflake {
                    |  groupId = 'io.github.stewartbryson'
                    |  artifactId = 'test-gradle-snowflake'
                    |  role = '$role'
                    |  database = '$database'
                    |  schema = '$schema'
                    |  warehouse = '$warehouse'
                    |  ephemeralName = '$ephemeralName'
                    |  applications {
                    |      add_numbers {
                    |         inputs = ["a integer", "b integer"]
                    |         returns = "string"
                    |         handler = "Sample.addNum"
                    |         immutable = true
                    |      }
                    |   }
                    |}
                    |version='0.1.0'
                    |""".stripMargin())
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", internalStage, '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm with ephemeral"() {
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
