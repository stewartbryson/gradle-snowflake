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
      dependsOn project.tasks.publish
   }

   @Optional
   @Input
   @Option(option = "account",
           description = "The URL of the Snowflake account."
   )
   String account = project.extensions."$PLUGIN".account

   @Optional
   @Input
   @Option(option = "user",
           description = "The user to connect to Snowflake."
   )
   String user = project.extensions."$PLUGIN".user

   @Optional
   @Input
   @Option(option = "password",
           description = "The password to connect to Snowflake."
   )
   String password = project.extensions."$PLUGIN".password

   @Optional
   @Input
   @Option(option = "database",
           description = "The Snowflake database to use."
   )
   String database = project.extensions."$PLUGIN".database

   @Input
   @Option(option = "schema",
           description = "The Snowflake schema to use."
   )
   String schema = project.extensions."$PLUGIN".schema

   @Input
   @Option(option = "warehouse",
           description = "The Snowflake warehouse to use."
   )
   String warehouse = project.extensions."$PLUGIN".warehouse

   @Input
   @Option(option = "role",
           description = "The Snowflake role to use."
   )
   String role = project.extensions."$PLUGIN".role

   @TaskAction
   def publish() {
      Map props = [
              url      : account,
              user     : user,
              password : password,
              role     : role,
              warehouse: warehouse,
              db       : database,
              schema   : schema
      ]
      log.warn "Props: ${props?.remove(password)}"
      try {
         Session session = Session.builder().configs(props).create()
      } catch (NullPointerException npe) {
         throw new Exception("Snowflake connection details are missing.")
      }

   }
}
