package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * Functional Java tests with GCS for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class JavaGoogleTest extends GradleSpec {

   @Shared
   String publishUrl = System.getProperty("gcsPublishUrl")


   def setupSpec() {
      stage = System.getProperty("gcsStage")
      writeBuildFile('java')

      appendBuildFile("""
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

   def "snowflakeJvm with GCS stage"() {
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
