package io.noumenal

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@Slf4j
@CacheableTask
class SnowflakePublish extends DefaultTask {
   private static String PLUGIN = 'snowflake'

   SnowflakePublish() {
      description = "Publish a Java artifact to an external stage and create Snowflake Functions and Procedures."
      group = "publishing"
      //dependsOn project.tasks.publish
   }

   @Optional
   @Input
   @Option(option = "account",
           description = "The URL of the Snowflake account."
   )
   String account = project.extensions."$PLUGIN".account

   @TaskAction
   def publish() {
      Map props = [
              url      : account,
              user     : project.extensions.snowflake.user,
              password : project.extensions.snowflake.password,
              role     : project.extensions.snowflake.role,
              warehouse: project.extensions.snowflake.warehouse,
              db       : project.extensions.snowflake.database,
              schema   : project.extensions.snowflake.schema
      ]
      log.warn "Props: $props"
      try {
         Session session = Session.builder().configs(props).create()
      } catch (NullPointerException npe) {
         throw new Exception("Snowflake connection details are missing.")
      }

   }
}
