package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import org.gradle.testkit.runner.GradleRunner

/**
 * Functional Java tests for the 'io.github.stewartbryson.snowflake' plugin.
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
    String ephemeralName = ('ephemeral_' + RandomStringUtils.randomAlphanumeric(9)), language = 'java', connection = 'gradle_plugin', stage = 'upload'

    def setupSpec() {
        settingsFile = new File(projectDir, 'settings.gradle')
        settingsFile.write("""
                     |rootProject.name = 'unit-test'
                     |""".stripMargin())

        buildFile = new File(projectDir, 'build.gradle')
        buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id "$language"
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |snowflake {
                    |  connection = '$connection'
                    |  stage = '$stage'
                    |  ephemeralName = '$ephemeralName'
                    |  useEphemeral = true
                    |  keepEphemeral = true
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

        javaFile = new File("${projectDir}/src/main/$language", "Sample.$language")
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
        args.add(0, taskName)

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

    def "snowflakeJvm with defaults"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    def "snowflakeJvm with custom JAR"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ['--jar', 'build/libs/unit-test-0.1.0-all.jar', '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    def "snowflakeJvm with immutable function"() {
        given:
        buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id '$language'
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |snowflake {
                    |  connection = '$connection'
                    |  stage = '$stage'
                    |  ephemeralName = '$ephemeralName'
                    |  useEphemeral = true
                    |  keepEphemeral = true
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
        result = executeSingleTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    // drop the ephemeral clone at the end
    def cleanupSpec() {
        executeSingleTask('dropEphemeral', ['-Si'])
    }
}
