package io.github.stewartbryson

import groovy.util.logging.Slf4j

/**
 * Functional Kotlin tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class KotlinTest extends GradleSpec {

   def setupSpec() {
      writeBuildFile('kotlin')

      appendBuildFile("""
                    |snowflake {
                    |  connection = '$connection'
                    |  ephemeralName = '$ephemeralName'
                    |  useEphemeral = true
                    |  keepEphemeral = true
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

      writeSourceFile('Sample', '''|
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

   def "shadowJar"() {
      given:
      taskName = 'shadowJar'

      when:
      result = executeTask(taskName, ['-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "snowflakeJvm for Kotlin"() {
      given:
      taskName = 'snowflakeJvm'

      when:
      result = executeTask(taskName, ["--stage", stage, '-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
   }

   // drop the ephemeral clone at the end
   def cleanupSpec() {
      executeTask('dropEphemeral', ['-Si'])
   }
}
