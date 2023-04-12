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

      project.ext.session = new Snowflake()

      project."$PLUGIN".extensions.applications = project.container(ApplicationContainer)
      project.apply plugin: 'com.redpillanalytics.gradle-properties'
      project.apply plugin: 'com.github.johnrengelman.shadow'
      project.apply plugin: 'java'
      project.pluginProps.setParameters(project, PLUGIN)

      project.afterEvaluate {
         log.info "Emphemeral clone name: ${extension.ephemeralName}"

         // add shadowJar to build
         project.build.dependsOn project.shadowJar
         // exclude some things in shadowJar
         project.shadowJar {
            dependencies {
               exclude(dependency('com.snowflake:snowpark:.*'))
            }
         }

         if (extension.useCustomMaven || extension.publishUrl) {
            // assert that we have artifact and group
            if (!extension.artifactId || !extension.groupId) {
               throw new Exception("'artifactId' and 'groupId' must be configured when publishing to external stages.")
            }
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

         // ephemeral tasks
         project.tasks.register("createEphemeral", CreateCloneTask)
         project.tasks.register("dropEphemeral", DropCloneTask)

         // if there is a functionalTest defined, it depends on snowflakeJvm
         if (project.tasks.findByName(extension.testSuite)) {
            project.tasks."${extension.testSuite}".dependsOn project.tasks.snowflakeJvm
         }

         // if an ephemeral environment is being used, then some tasks need dependencies
         if (extension.useEphemeral) {
            project.tasks.snowflakeJvm.configure {
               // snowflakeJvm should always run when using ephemeral clones
               // that's because the clone may be dropped at the end of the last run
               //TODO #88
               outputs.upToDateWhen {false}
               // clone should be created before publishing
               dependsOn project.tasks.createEphemeral
            }

            // if there is a functionalTest defined, clone before running tests
            if (project.tasks.findByName(extension.testSuite)) {
               project.tasks."${extension.testSuite}".dependsOn project.tasks.createEphemeral
            }
            // if we aren't keeping the ephemeral environment at the end of the run
            if (!extension.keepEphemeral) {
               // publishing should be followed by dropping the clone
               project.tasks.snowflakeJvm.finalizedBy project.tasks.dropEphemeral

               // if there is a functionalTest suite defined, run it before dropping the clone
               if (project.tasks.findByName(extension.testSuite)) {
                  //project.tasks."${extension.testSuite}".finalizedBy project.tasks.dropEphemeral
                  project.tasks.dropEphemeral.mustRunAfter project.tasks."${extension.testSuite}"
               }
            }
         }
      }
   }
}
