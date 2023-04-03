package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * A simple unit test for the 'io.github.stewartbryson.snowflake' plugin.
 */
@Slf4j
class BuildTest extends GradleSpec {

   @Shared
   String publishUrl = System.getProperty("s3PublishUrl")

   def setupSpec() {
      language = 'java'
      buildFile.append("""
                    |snowflake {
                    |  groupId = 'io.github.stewartbryson'
                    |  artifactId = 'test-gradle-snowflake'
                    |  connection = '$connection'
                    |  applications {
                    |      add_numbers {
                    |         inputs = ["a integer", "b integer"]
                    |         returns = "string"
                    |         handler = "Sample.addNum"
                    |      }
                    |   }
                    |}
                    |""".stripMargin())
      writeSourceFile('java','Sample',"""
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

   def "tasks"() {
      given:
      taskName = 'tasks'

      when:
      result = executeTask(taskName, ['-S'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "help task for SnowflakeJvm"() {
      given:
      taskName = 'help'

      when:
      result = executeTask(taskName, ['--task', 'snowflakeJvm', '-S'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "tasks for publishing group with publishUrl"() {
      given:
      taskName = 'tasks'

      when:
      result = executeTask(taskName, ['--group', 'publishing', '-S', "-Psnowflake.publishUrl=$publishUrl".toString()])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "dry run without publishUrl"() {
      given:
      taskName = 'snowflakeJvm'

      when:
      result = executeTask(taskName, ['-Sim'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }

   def "shadowJar"() {
      given:
      taskName = 'shadowJar'

      when:
      result = executeTask(taskName, ['-Si'])

      then:
      !result.tasks.collect { it.outcome }.contains('FAILURE')
   }
}
