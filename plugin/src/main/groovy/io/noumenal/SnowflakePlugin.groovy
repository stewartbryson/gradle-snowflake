package io.noumenal

import groovy.util.logging.Slf4j
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.aws.AwsImAuthentication
import scala.App

@Slf4j
class SnowflakePlugin implements Plugin<Project> {
   private static String PLUGIN = 'snowflake'

   void apply(Project project) {
      project.extensions.create(PLUGIN, SnowflakeExtension)
      project."$PLUGIN".extensions.applications = project.container(ApplicationContainer)
      project.apply plugin: 'com.redpillanalytics.gradle-properties'
      project.apply plugin: 'maven-publish'
      project.apply plugin: 'java-library'
      project.apply plugin: 'com.github.johnrengelman.shadow'
      project.pluginProps.setParameters(project, PLUGIN)

      project.afterEvaluate {
         // create maven publishing
         if (!project.extensions."$PLUGIN".useCustomMaven) {
            //log.warn "Publish: ${project.extensions."$PLUGIN".publishUrl}"

            // create publication
            project.publishing.publications {
               snowflake(MavenPublication) {
                  groupId = project.extensions."$PLUGIN".groupId
                  artifactId = project.extensions."$PLUGIN".artifactId
                  //from project.components.java
                  artifact project.shadowJar
               }
            }
            // create repository
            project.publishing.repositories {
               maven {
                  name project.extensions."$PLUGIN".stage
                  url project.extensions."$PLUGIN".publishUrl
                  authentication {
                     awsIm(AwsImAuthentication)
                  }
               }
            }
         }

         // Register a task
         project.tasks.register("snowflakePublish", SnowflakePublish)
         // set dependency
         if (!project.extensions.snowflake.useCustomMaven) {
            project.tasks.snowflakePublish.dependsOn project.extensions."$PLUGIN".publishTask
         }

         // for debugging
         def snowflake = project.extensions."$PLUGIN"
         def pubs = project.publishing.publications
         def repos = project.publishing.repositories
         def pubTask = project.extensions."$PLUGIN".publishTask
         def text = "text"
      }
   }
}
