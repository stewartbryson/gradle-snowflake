package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.TempDir

/**
 * Functional Java tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
@Stepwise
class EphemeralTest extends Specification {
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
    String ephemeralName = 'ephemeral_unit_test', language = 'java', connection = 'gradle_plugin', stage = 'upload'

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
                    |  connection = '$connection'
                    |  stage = '$stage'
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

    def "create ephemeral clone"() {
        given: "A taskname called 'createEphemeral'."
        taskName = 'createEphemeral'

        when: "The task is executed"
        result = executeSingleTask(taskName, ['-Si'])

        then: "We get a successful execution with 'Ephemeral <clone name> created' in the output"
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    def "drop ephemeral clone"() {
        given: "A taskname called 'dropEphemeral'."
        taskName = 'dropEphemeral'

        when: "The task is executed"
        result = executeSingleTask(taskName, ['-Si'])

        then: "We get a successful execution with 'Ephemeral <clone name> created' in the output"
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(dropped)(.+)/)
    }
}
