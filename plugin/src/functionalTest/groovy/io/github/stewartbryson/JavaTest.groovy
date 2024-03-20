package io.github.stewartbryson

import groovy.util.logging.Slf4j

/**
 * Functional Java tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class JavaTest extends GradleSpec {

   def setupSpec() {
      writeBuildFile('java')
      appendBuildFile("""
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
                    |         runtime = '17'
                    |         handler = "Sample.addNum"
                    |      }
                    |   }
                    |}
                    |""")

      writeSourceFile('Sample', """
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
                  |""")
   }

   def "snowflakeJvm with defaults"() {
      given:
      taskName = 'snowflakeJvm'

      when:
      result = executeTask(taskName, ['-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
   }

   def "snowflakeJvm with custom JAR"() {
      given:
      taskName = 'snowflakeJvm'

      when:
      result = executeTask(taskName, ['--jar', 'build/libs/unit-test-0.1.0-all.jar', '-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
   }

   def "snowflakeJvm with immutable function"() {
      given: "A new snowflake closure with 'immutable'."
      writeBuildFile('java')
      appendBuildFile("""
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
                    |         runtime = '17'
                    |         handler = "Sample.addNum"
                    |         immutable = true
                    |      }
                    |   }
                    |}
                    |""")
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
