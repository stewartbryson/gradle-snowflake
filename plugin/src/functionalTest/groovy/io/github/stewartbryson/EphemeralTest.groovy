package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Stepwise

/**
 * Functional Java tests for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
@Stepwise
class EphemeralTest extends GradleSpec {

   def setupSpec() {
      writeBuildFile('java')
      appendBuildFile("""
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
                  |""".stripMargin())
      log.warn buildFile.text
   }

   def "create ephemeral clone"() {
      given: "A taskname called 'createEphemeral'."
      taskName = 'createEphemeral'

      when: "The task is executed"
      result = executeTask(taskName, ['-Si'])

      then: "We get a successful execution with 'Ephemeral <clone name> created' in the output"
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(created)(.+)/)
   }

   def "drop ephemeral clone"() {
      given: "A taskname called 'dropEphemeral'."
      taskName = 'dropEphemeral'

      when: "The task is executed"
      result = executeTask(taskName, ['-Si'])

      then: "We get a successful execution with 'Ephemeral <clone name> created' in the output"
      !result.tasks.collect { it.outcome }.contains('FAILURE')
      result.output.matches(/(?ms)(.+)(Ephemeral clone)(.+)(dropped)(.+)/)
   }
}
