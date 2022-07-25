package io.noumenal

import org.gradle.api.Project

class SnowflakeExtension {
   SnowflakeExtension(Project project) {
      this.project = project
   }
   private Project project

   Map getProperties() {
      [
              url      : account,
              user     : user,
              password : password,
              role     : role,
              warehouse: warehouse,
              db       : database,
              schema   : schema
      ]
   }

   String account
   String user
   String password
   String database
   String schema = 'public'
   String role = 'sysadmin'
   String warehouse = "compute_wh"
   String stage
}