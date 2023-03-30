package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * A superclass for creating Gradle tasks that work with Snowflake.
 */
@Slf4j
@CacheableTask
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
     * The Snowsql config file to use. Default: Looks first for '~/.snowsql/config' followed by './snowconfig'.
     */
    @Input
    @Optional
    @Option(option = "snow-config",
            description = "Custom Snowsql config file."
    )
    String snowConfig

    /**
     * The Snowsql connection to use.
     */
    @Input
    @Optional
    @Option(option = "snow-config",
            description = "Custom Snowsql config file."
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
        if (snowConfig) {
            snowflake = new Snowflake(project.file(snowConfig), connection)
        } else {
            snowflake = new Snowflake(connection)
        }
    }

    /**
     * Return the first column from the first row of a SELECT statement.
     *
     * @return a scalar column value.
     */
    def getScalarValue(String sql) {
        return snowflake.getScalarValue(sql)
    }
}
