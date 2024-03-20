package io.github.stewartbryson

import groovy.util.logging.Slf4j

/**
 * Functional Groovy tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class GroovyTest extends GradleSpec {

    def setupSpec() {
        writeBuildFile('groovy')
        appendBuildFile("""
                    |dependencies {
                    |    implementation 'org.codehaus.groovy:groovy:3.0.13'
                    |}
                    |snowflake {
                    |  ephemeralName = '$ephemeralName'
                    |  useEphemeral = true
                    |  keepEphemeral = true
                    |  connection = '$connection'
                    |  stage = '$stage'
                    |  applications {
                    |      add_numbers {
                    |         inputs = ["a integer", "b integer"]
                    |         returns = "string"
                    |         runtime = '17'
                    |         handler = "Sample.addNum"
                    |      }
                    |   }
                    |}
                    |version='0.1.0'
                    |""")

        writeSourceFile('Sample','''|
                        |class Sample {
                        |  String addNum(Integer num1, Integer num2) {
                        |    try {
                        |      "Sum is: ${(num1 + num2)}"
                        |    } catch (Exception e) {
                        |      null
                        |    }
                        |  }
                        |}
                        |''')
    }

    def "shadowJar"() {
        given:
        taskName = 'shadowJar'

        when:
        result = executeTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
    }

    def "snowflakeJvm for Groovy"() {
        given:
        taskName = 'snowflakeJvm'

        when:
        result = executeTask(taskName, ['-Si'])

        then:
        !result.tasks.collect { it.outcome }.contains('FAILURE')
        result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
    }

    // drop the ephemeral clone at the end
    def cleanupSpec() {
        executeTask('dropEphemeral', ['-Si'])
    }
}
