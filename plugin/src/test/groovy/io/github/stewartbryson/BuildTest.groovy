package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * A simple unit test for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class BuildTest extends Specification {
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

    def "help task for SnowflakeJvm"() {
        given:
        taskName = 'help'

        when:
        result = executeSingleTask(taskName, ['--task','snowflakeJvm','-S'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "tasks for publishing group with publishUrl"() {
        given:
        taskName = 'tasks'

        when:
        result = executeSingleTask(taskName, ['--group','publishing','-S',"-Psnowflake.publishUrl=$s3PublishUrl".toString()])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "dry run without publishUrl"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ['-Sim'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "shadowJar"() {
        given:
        taskName = 'shadowJar'

        when:
        result = executeSingleTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }
}
