package io.github.stewartbryson

import groovy.util.logging.Slf4j

/**
 * Functional Scala tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class ScalaTest extends GradleSpec {

   def setupSpec() {
      writeBuildFile('scala')

      appendBuildFile("""
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
                    |""")

      writeSourceFile('Sample', """|
                   |class Sample {
                   |  def addNum(num1: Integer, num2: Integer): String = {
                   |    try {
                   |      "Sum is: " + (num1 + num2).toString()
                   |    } catch {
                   |      case e: Exception => return null
                   |    }
                   |  }
                   |}
                   |""")
   }

   def "shadowJar"() {
      given:
      taskName = 'shadowJar'

      when:
      result = executeTask(taskName, ['-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "snowflakeJvm for Scala"() {
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
