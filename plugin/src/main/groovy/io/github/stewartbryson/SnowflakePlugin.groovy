package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.aws.AwsImAuthentication
/**
 * A Gradle plugin for publishing UDFs to Snowflake.
 */
@Slf4j
class SnowflakePlugin implements Plugin<Project> {
   private static String PLUGIN = 'snowflake'

   /**
    * Apply the snowflake plugin to a Gradle project. Also applies the 'com.github.johnrengelman.shadow' plugin.
    */
   void apply(Project project) {
      project.extensions.create(PLUGIN, SnowflakeExtension, project)

      // shorthand
      def extension = project.extensions."$PLUGIN"

      project."$PLUGIN".extensions.applications = project.container(ApplicationContainer)
      project.apply plugin: 'com.redpillanalytics.gradle-properties'
      project.apply plugin: 'com.github.johnrengelman.shadow'
      project.pluginProps.setParameters(project, PLUGIN)

      project.afterEvaluate {
         log.info "Emphemeral clone name: ${extension.ephemeral}"

         // add shadowJar to build
         project.build.dependsOn project.shadowJar

         if (extension.useCustomMaven || extension.publishUrl) {
            // assert that we have artifact and group
            assert (extension.artifactId && extension.groupId)
         }

         // create maven publishing
         if (!extension.useCustomMaven && extension.publishUrl) {

            // apply the maven-publish plugin for the user
            project.apply plugin: 'maven-publish'

            // create publication
            project.publishing.publications {
               snowflake(MavenPublication) {
                  groupId = extension.groupId
                  artifactId = extension.artifactId
                  artifact project.shadowJar
               }
            }
            // create repository
            // check and see if we are AWS or GCS
            if (extension.publishUrl ==~ /(?i)(s3:\/\/)(.+)/) {
               project.publishing.repositories {
                  maven {
                     name extension.stage
                     url extension.publishUrl
                     authentication {
                        awsIm(AwsImAuthentication)
                     }
                  }
               }
            } else {
               project.publishing.repositories {
                  maven {
                     name extension.stage
                     url extension.publishUrl
                  }
               }
            }
         }

         // Register snowflakePublish task
         project.tasks.register("snowflakeJvm", SnowflakeJvm)
         // construct the lifecycle task
         project.tasks.register("snowflakePublish") {
            description "Lifecycle task for all Snowflake publication tasks."
            group "publishing"
            dependsOn project.snowflakeJvm
         }
         // set dependency
         if (!extension.useCustomMaven && extension.publishUrl) {
            project.snowflakeJvm.dependsOn extension.publishTask
            project.tasks.getByName(extension.publishTask).mustRunAfter project.test
         }
         project.snowflakeJvm.dependsOn project.test, project.shadowJar
      }
   }
}
