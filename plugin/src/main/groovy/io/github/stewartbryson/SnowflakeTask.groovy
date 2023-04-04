package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * A superclass for creating Gradle tasks that work with Snowflake.
 */
@Slf4j
abstract class SnowflakeTask extends DefaultTask {

    @Internal
    String PLUGIN = 'snowflake'

    /**
     * A helper for getting the plugin extension.
     *
     * @return A reference to the plugin extension.
     */
    @Internal
    def getExtension() {
        project.extensions."$PLUGIN"
    }

    /**
     * The Snowsql config file to use. Default: Looks first for '~/.snowsql/config' followed by './snow-config'.
     */
    @Input
    @Optional
    @Option(option = "snow-config",
            description = "Custom Snowsql config file."
    )
    String snowConfig

    /**
     * The Snowsql connection to use. Default: use the base connection info in Snowsql config.
     */
    @Input
    @Option(option = "snow-config",
            description = "Custom Snowsql connection to use."
    )
    String connection = extension.connection

    /**
     * The {@link Snowflake} object.
     */
    @Internal
    Snowflake snowflake

    /**
     * Create a Snowflake session.
     *
     * @return a Snowflake session.
     */
    def createSession() {
        // reuse an existing connection if it already exists
        if (!project.session.hasSession()) {
            if (snowConfig) {
                snowflake = new Snowflake(project.file(snowConfig), connection)
            } else {
                snowflake = new Snowflake(connection)
            }
            project.session = snowflake
        } else {
            log.warn "Reusing existing Snowflake session."
            snowflake = project.session
        }
    }
}
