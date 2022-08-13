package io.noumenal

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j
import net.snowflake.client.jdbc.SnowflakeStatement
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.sql.ResultSet
import java.sql.Statement

@Slf4j
@CacheableTask
class SnowflakePublish extends DefaultTask {
   private static String PLUGIN = 'snowflake'

   @Internal
   def getExtension() {
      project.extensions."$PLUGIN"
   }

   SnowflakePublish() {
      description = "Publish a Java artifact to an external stage and create Snowflake Functions and Procedures."
      group = "publishing"
   }

   @Optional
   @Input
   @Option(option = "account",
           description = "The URL of the Snowflake account."
   )
   String account = extension.account

   @Optional
   @Input
   @Option(option = "user",
           description = "The user to connect to Snowflake."
   )
   String user = extension.user

   @Optional
   @Input
   @Option(option = "password",
           description = "The password to connect to Snowflake."
   )
   String password = extension.password

   @Optional
   @Input
   @Option(option = "database",
           description = "The Snowflake database to use."
   )
   String database = extension.database

   @Input
   @Option(option = "schema",
           description = "The Snowflake schema to use."
   )
   String schema = extension.schema

   @Input
   @Option(option = "warehouse",
           description = "The Snowflake warehouse to use."
   )
   String warehouse = extension.warehouse

   @Input
   @Option(option = "role",
           description = "The Snowflake role to use."
   )
   String role = extension.role

   @Optional
   @Input
   @Option(option = "stage",
           description = "The Snowflake external stage to publish to."
   )
   String stage = extension.stage

   @Optional
   @Input
   @Option(option = "publishUrl",
           description = "The url of the Snowflake external stage to publish to."
   )
   String publishUrl = extension.publishUrl

   @Internal
   Session getSession() {
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
      return session
   }

   @OutputFile
   File output = project.file("${project.buildDir}/${PLUGIN}/output.txt")

   String getImports(Session session) {
      String basePath = "@${stage}/${extension.groupId.replace('.', '/')}/${extension.artifactId}/${project.version}"
      //log.warn "basePath: $basePath"
      Statement statement = session.jdbcConnection().createStatement()
      String sql = "LIST $basePath pattern='(.)*(-all)\\.jar'; select * from table(result_scan(last_query_id())) order by 'last_modified' asc;"
      statement.unwrap(SnowflakeStatement.class).setParameter(
              "MULTI_STATEMENT_COUNT", 2)
      ResultSet rs = statement.executeQuery(sql)
      String fileName
      String filePath
      try {
         while (rs.next()) {
            filePath = rs.getString(1)
         }
         fileName = filePath.replaceAll(/(.*)($project.version)(\/)(.*)/) { all, first, version, slash, filename ->
            filename
         }
      } catch (Exception e) {
         throw new Exception("Unable to detect the correct JAR in stage '${stage}'.")
      }
      rs.close()
      statement.close()
      "'$basePath/$fileName'"
   }

   @TaskAction
   def publish() {
      // keep the session
      Session session = this.session

      // ensure that the stage and the publishUrl are aligned
      Statement statement = session.jdbcConnection().createStatement()
      String sql = "select stage_url from information_schema.stages where stage_name=upper('$stage') and stage_schema=upper('$schema') and stage_type='External Named'"
      ResultSet rs = statement.executeQuery(sql)
      String selectStage
      if (rs.next()) {
         selectStage = rs.getString(1)
      }
      assert selectStage == publishUrl

      // create snowflake application

      // automatically create application spec objects
      output.write("Snowflake Application:\n\n")
      project."$PLUGIN".applications.each { ApplicationContainer app ->
         String createText = app.getCreate(getImports(session))
         String message = "Deploying ==> \n$createText"
         log.warn message
         output.append("$message\n")
         session.jdbcConnection().createStatement().execute(createText)
      }
      session.close()
   }
}
