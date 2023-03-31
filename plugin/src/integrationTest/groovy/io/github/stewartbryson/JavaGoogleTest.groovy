package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Functional Java tests with GCS for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class JavaGoogleTest extends Specification {
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
    String ephemeralName = ('ephemeral_' + RandomStringUtils.randomAlphanumeric(9)), connection = 'gradle_plugin'

    @Shared
    String publishUrl = System.getProperty("gcsPublishUrl"),
           stage = System.getProperty("gcsStage")

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
                    |  connection = '$connection'
                    |  stage = '$stage'
                    |  publishUrl = '$publishUrl'
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

    def "snowflakeJvm with GCS stage"() {
        given:
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
