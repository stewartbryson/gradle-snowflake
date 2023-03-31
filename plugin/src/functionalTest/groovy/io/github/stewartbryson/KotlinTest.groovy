package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Functional Kotlin tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class KotlinTest extends Specification {
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
    String ephemeralName = ('ephemeral_' + RandomStringUtils.randomAlphanumeric(9)), language = 'kotlin', connection = 'gradle_plugin', stage = 'upload'

    def setupSpec() {
        settingsFile = new File(projectDir, 'settings.gradle')
        settingsFile.write("""
                     |rootProject.name = "$language-test"
                     |""".stripMargin())

        buildFile = new File(projectDir, 'build.gradle')
        buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id "org.jetbrains.kotlin.jvm" version "1.7.21"
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |repositories {
                    |    mavenCentral()
                    |}
                    |snowflake {
                    |  connection = '$connection'
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

        classFile = new File("${projectDir}/src/main/$language", "Sample.kt")
        classFile.parentFile.mkdirs()
        classFile.write('''|
                            |class Sample {
                            |  fun addNum(num1: Int, num2: Int): String {
                            |    try {
                            |      return "Sum is: " + (num1 + num2).toString()
                            |    } catch (e: Exception) {
                            |      return null.toString()
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

    def "snowflakeJvm for Kotlin"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeSingleTask(taskName, ["--stage", stage, '-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    // drop the ephemeral clone at the end
    def cleanupSpec() {
        executeSingleTask('dropEphemeral', ['-Si'])
    }
}
