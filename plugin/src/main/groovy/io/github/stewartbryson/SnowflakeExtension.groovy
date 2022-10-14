package io.github.stewartbryson

import org.apache.commons.lang3.RandomStringUtils
import org.gradle.api.Project


/**
 * The plugin configuration extension that is applied to the Gradle project as 'snowflake'.
 */
class SnowflakeExtension {
   SnowflakeExtension(Project project) {
      this.project = project
   }
   private Project project

   /**
    * The Snowflake account URL, for instance: https://gradle-snowflake.us-east-1.snowflakecomputing.com:443.
    */
   String account
   /**
    * The Snowflake user to connect as.
    */
   String user
   /**
    * The Snowflake password to connect with.
    */
   String password
   /**
    * The Snowflake database to connect to.
    */
   String database
   /**
    * The Snowflake schema to connect with. Default: 'public'.
    */
   String schema = 'public'
   /**
    * The Snowflake role to connect with.
    */
   String role
   /**
    * The Snowflake warehouse to connect with. Default: 'compute_wh'.
    */
   String warehouse = "compute_wh"
   /**
    * The Snowflake stage to upload to. Default: 'maven'.
    */
   String stage = 'maven'
   /**
    * Optional: specify the URL of {@link #stage} if it is external. The plugin will apply the 'maven-publish' plugin and handle publishing artifacts there.
    */
   String publishUrl
   /**
    * Optional: specify an artifact groupId when using the 'maven-publish' plugin.
    */
   String groupId = project.getGroup()
   /**
    * Optional: specify an artifactId when using the 'maven-publish' plugin.
    */
   String artifactId = project.getName()
   /**
    * Optional: do not automatically apply 'maven-publish' and allow the user to apply that plugin in the 'build.gradle' file.
    */
   Boolean useCustomMaven = false
   /**
    * Create and use an ephemeral Snowflake clone for the entire build. Useful for CI/CD processes. Default: false
    */
   Boolean useClone = false
   /**
    * Automatically drop ephemeral Snowflake clone at the end of the build. Default: false
    */
   Boolean dropClone = false
   /**
    * The name of the cloned Snowflake database. Default: auto-generated.
    *
    * When GitHub Actions environment variables are available, then an intelligent name is generated based on the type of event. When GitHub actions environment variables are not available, a simple unique name is generated.
    */
   String cloneName = snowflakeCloneName

//   /**
//    * Convert names to be Snake Case.
//    */
//   private static String toSnakeCase( String text ) {
//      text.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
//   }

//   /**
//    * Convert names to be Camel Case.
//    */
//   private static String toCamelCase( String text, boolean capitalized = false ) {
//      text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
//      return capitalized ? capitalize(text) : text
//   }

   /**
    * Return the name of the Maven publication task associated with the external stage.
    */
   String getPublishTask() {
      //toCamelCase("publish_snowflake_publication_to_${stage}Repository")
      "publishSnowflakePublicationTo${stage.capitalize()}Repository"
   }

   /**
    * Return the database clone name based on GitHub actions when available.
    */
   String getSnowflakeCloneName() {
      String refName = System.getenv('GITHUB_REF_NAME')
      String refType = refName.endsWith('/merge') ? 'pr' : System.getenv('$GITHUB_REF_TYPE')
      String baseName = refName.replaceAll(/\/\w+/,'')
      String cloneName = "ephemeral_" + (System.getenv('GITHUB_ACTIONS') ? "${refType}_${baseName}" : RandomStringUtils.randomAlphanumeric(9))
   }
}