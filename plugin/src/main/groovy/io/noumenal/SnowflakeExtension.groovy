package io.noumenal

import org.gradle.api.Project
class SnowflakeExtension {
   SnowflakeExtension(Project project) {
      this.project = project
   }
   private Project project

   String account
   String user
   String password
   String database
   String schema = 'public'
   String role
   String warehouse = "compute_wh"
   String stage = 'maven'
   String publishUrl
   String groupId = project.getGroup()
   String artifactId = project.getName()
   Boolean useCustomMaven = false

   private static String toSnakeCase( String text ) {
      text.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
   }

   private static String toCamelCase( String text, boolean capitalized = false ) {
      text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
      return capitalized ? capitalize(text) : text
   }

   String getPublishTask() {
      toCamelCase("publish_snowflake_publication_to_${stage}Repository")
   }
}