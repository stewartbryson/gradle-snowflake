package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir


@Slf4j
class GradleSpec extends Specification {
   @Shared
   def result

   @Shared
   String taskName, language

   @TempDir
   @Shared
   File projectDir

   @Shared
   File buildFile, settingsFile, mainDir

   @Shared
   String connection = 'gradle_plugin', stage = 'upload'

   @Shared
   String ephemeralName = ('ephemeral_' + RandomStringUtils.randomAlphanumeric(9))

   def setupSpec() {
      settingsFile = new File(projectDir, 'settings.gradle')
      settingsFile.write("""
                     |rootProject.name = 'plugin-unit-test'
                     |""".stripMargin())

      buildFile = new File(projectDir, 'build.gradle')
      mainDir = new File(projectDir, "src/main/")
   }

   def writeSourceFile(String className, String text) {
      def extension = (language == 'kotlin' ? 'kt' : language)
      def sourceFile = new File(mainDir, "${language}/${className}.${extension}")
      sourceFile.parentFile.mkdirs()
      sourceFile.write(text.stripMargin())
      log.info "Source file: $sourceFile"
   }

   def appendBuildFile(String text) {
      buildFile.append(text.stripMargin())
   }

   def writeBuildFile(String language) {
      this.language = language
      def plugin = (language == 'kotlin' ? /"org.jetbrains.kotlin.jvm" version "1.7.21"/ : "'${language}'")
      log.warn "Plugin: $plugin"
      buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id ${plugin}
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |repositories {
                    |    mavenCentral()
                    |}
                    |version='0.1.0'
                    |""".stripMargin())
      log.warn "$buildFile"
   }

   def executeTask(String taskName, List args, Boolean logOutput = true) {
      args.add(0, taskName)

      // execute the Gradle test build
      result = GradleRunner.create()
              .withProjectDir(projectDir)
              .withArguments(args)
              .withPluginClasspath()
              .forwardOutput()
              .build()

      // log the results
      if (logOutput) log.warn result.getOutput()
      return result
   }
}
