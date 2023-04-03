package io.github.stewartbryson

import groovy.util.logging.Slf4j
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
   File buildFile, settingsFile, mainDir, sourceFile

   @Shared
   String connection = 'gradle_plugin'

   def setupSpec() {
      settingsFile = new File(projectDir, 'settings.gradle')
      settingsFile.write("""
                     |rootProject.name = 'plugin-unit-test'
                     |""".stripMargin())

      buildFile = new File(projectDir, 'build.gradle')
      buildFile.write("""
                    |plugins {
                    |    id 'io.github.stewartbryson.snowflake'
                    |    id 'java'
                    |}
                    |java {
                    |    toolchain {
                    |        languageVersion = JavaLanguageVersion.of(11)
                    |    }
                    |}
                    |version='0.1.0'
                    |""".stripMargin())
      mainDir = new File(projectDir, "src/main/")
   }

   def getSourceDir() {
      return new File(mainDir, language)
   }

   def writeSourceFile(String className, String text) {
      def sourceFile = new File(mainDir, "${language}/${className}.${language}")
      sourceFile.parentFile.mkdirs()
      sourceFile.write(text.stripMargin())
      log.info "Source file: $sourceFile"
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
