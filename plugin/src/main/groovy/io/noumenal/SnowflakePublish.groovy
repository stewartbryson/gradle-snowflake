package io.noumenal

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.sql.ResultSet
import java.sql.Statement

@Slf4j
@CacheableTask
class SnowflakePublish extends DefaultTask {
   private static String PLUGIN = 'snowflake'

   SnowflakePublish() {
      description = "Publish a Java artifact to an external stage and create Snowflake Functions and Procedures."
      group = "publishing"
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

   @Optional
   @Input
   @Option(option = "stage",
           description = "The Snowflake external stage to publish to."
   )
   String stage = project.extensions."$PLUGIN".stage

   @Optional
   @Input
   @Option(option = "publishUrl",
           description = "The url of the Snowflake external stage to publish to."
   )
   String publishUrl = project.extensions."$PLUGIN".publishUrl

   @TaskAction
   def publish() {
      //def handler = project.extensions."$PLUGIN".handler
      Map props = [
              url      : account,
              user     : user,
              password : password,
              role     : role,
              warehouse: warehouse,
              db       : database,
              schema   : schema
      ]
      Map printable = props.clone()
      printable.password = "*********"
      log.info "Snowflake config: $printable"
      Session session

      // get a Snowflake session
      try {
         session = Session.builder().configs(props).create()
      } catch (NullPointerException npe) {
         throw new Exception("Snowflake connection details are missing.")
      }

      // ensure that the stage and the publishUrl are aligned
      Statement statement = session.jdbcConnection().createStatement()
      String sql = "select stage_url from information_schema.stages where stage_name=upper('$stage') and stage_type='External Named'"
      ResultSet rs = statement.executeQuery(sql)
      String selectStage
      if(rs.next()){ selectStage = rs.getString(1)}
      assert selectStage == publishUrl

      // create snowflake application
      String imports = project.extensions."$PLUGIN".imports

      // automatically create application spec objects
      project."$PLUGIN".applications.each { ApplicationContainer app ->
         String createText = app.getCreate(imports)
         log.info "Deploying ==> \n$createText"
         session.jdbcConnection().createStatement().execute(createText)
      }
   }
}
