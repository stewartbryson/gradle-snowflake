package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * A Gradle task for provisioning a Snowflake clone for ephemeral testing.
 */
@Slf4j
abstract class CreateClone extends SnowflakeTask {

    /**
     * The task Constructor with 'description' and 'group'.
     *
     * @return A custom task class.
     */
    CreateClone() {
        description = "A Gradle task for provisioning a Snowflake clone for ephemeral testing."
        group = "verification"
    }

    /**
     * The Snowflake stage to publish to. Overrides {@link SnowflakeExtension#stage}.
     */
    @Optional
    @Input
    @Option(option = "stage",
            description = "Override the Snowflake stage to publish to."
    )
    String stage = extension.stage

    /**
     * Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.
     */
    @Optional
    @Input
    @Option(option = "jar", description = "Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.")
    String jar = project.tasks.shadowJar.archiveFile.get()

    /**
     * A simple text output file for the Snowflake applications create statements. Mainly for making the class Cacheable.
     */
    @OutputFile
    File output = project.file("${project.buildDir}/${PLUGIN}/output.txt")

    /**
     * The Gradle TaskAction method. Create the ephemeral clone.
     */
    @TaskAction
    def clone() {

        // close the session
        session.close()
    }
}
