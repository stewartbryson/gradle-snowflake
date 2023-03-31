package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Functional Scala tests for the 'io.github.stewartbryson.snowflake' plugin.
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
    File buildFile, settingsFile, classFile

    @Shared
    String ephemeralName = ('ephemeral_' + RandomStringUtils.randomAlphanumeric(9)), language = 'scala', connection = 'gradle_plugin', stage = 'upload'

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
                    |    implementation 'org.scala-lang:scala-library:2.13.10'
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

        classFile = new File("${projectDir}/src/main/$language", "Sample.$language")
        classFile.parentFile.mkdirs()
        classFile.write("""|
                            |class Sample {
                            |  def addNum(num1: Integer, num2: Integer): String = {
                            |    try {
                            |      "Sum is: " + (num1 + num2).toString()
                            |    } catch {
                            |      case e: Exception => return null
                            |    }
                            |  }
                            |}
                  |""".stripMargin())
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

    def "snowflakeJvm for Scala"() {
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
