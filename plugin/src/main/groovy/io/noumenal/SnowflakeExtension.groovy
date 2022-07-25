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
   String role = 'sysadmin'
   String warehouse = "compute_wh"
   String publication = 'snowflake'
}