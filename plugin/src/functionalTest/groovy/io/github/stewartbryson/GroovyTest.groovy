package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Functional Groovy tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class GroovyTest extends Specification {
    @Shared
    def result

    @Shared
    String taskName

    @TempDir
    @Shared
    private File projectDir

    @Shared
    File buildFile, settingsFile, classFile

    @Shared
    String ephemeralName = 'ephemeral_unit_test', language = 'groovy', connection = 'gradle_plugin', stage = 'upload'

    def setupSpec() {
        settingsFile = new File(projectDir, 'settings.gradle')
        settingsFile.write("""
                     |rootProject.name = "$language-test"
                     |""".stripMargin())

        buildFile = new File(projectDir, 'build.gradle')
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
                    |repositories {
                    |    mavenCentral()
                    |}
                    |dependencies {
                    |    implementation 'org.codehaus.groovy:groovy:3.0.13'
                    |}
                    |snowflake {
                    |  ephemeralName = '$ephemeralName'
                    |  connection = '$connection'
                    |  stage = '$stage'
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

        classFile = new File("${projectDir}/src/main/$language", "Sample.$language")
        classFile.parentFile.mkdirs()
        classFile.write('''|
                            |class Sample {
                            |  String addNum(Integer num1, Integer num2) {
                            |    try {
                            |      "Sum is: ${(num1 + num2)}"
                            |    } catch (Exception e) {
                            |      null
                            |    }
                            |  }
                            |}
                  |'''.stripMargin())
    }

    // helper method
    def executeSingleTask(String taskName, List args, Boolean logOutput = true) {
        args.add(0, taskName)

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

    def "snowflakeJvm for Groovy"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm for Groovy with ephemeral"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", stage, "--use-ephemeral", '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(dropped)(.+)/)
    }
}
