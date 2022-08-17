package io.noumenal

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.aws.AwsImAuthentication

@Slf4j
class SnowflakePlugin implements Plugin<Project> {
   private static String PLUGIN = 'snowflake'

   void apply(Project project) {
      project.extensions.create(PLUGIN, SnowflakeExtension)

      // shorthand
      def extension = project.extensions."$PLUGIN"

      project."$PLUGIN".extensions.applications = project.container(ApplicationContainer)
      project.apply plugin: 'com.redpillanalytics.gradle-properties'
      project.apply plugin: 'java-library'
      project.apply plugin: 'com.github.johnrengelman.shadow'
      project.pluginProps.setParameters(project, PLUGIN)

      project.afterEvaluate {
         // create maven publishing
         if (!extension.useCustomMaven && extension.publishUrl) {

            // apply the maven-publish plugin for the user
            project.apply plugin: 'maven-publish'

            // create publication
            project.publishing.publications {
               snowflake(MavenPublication) {
                  groupId = extension.groupId
                  artifactId = extension.artifactId
                  //from project.components.java
                  artifact project.shadowJar
               }
            }
            // create repository
            project.publishing.repositories {
               maven {
                  name extension.stage
                  url extension.publishUrl
                  authentication {
                     awsIm(AwsImAuthentication)
                  }
               }
            }
         }

         // Register a task
         project.tasks.register("snowflakePublish", SnowflakePublish)
         // set dependency
         if (!extension.useCustomMaven && extension.publishUrl) {
            project.tasks.snowflakePublish.dependsOn extension.publishTask
            project.tasks.getByName(extension.publishTask).mustRunAfter project.tasks.test
         }

         project.tasks.snowflakePublish.dependsOn project.tasks.test
      }
   }
}
