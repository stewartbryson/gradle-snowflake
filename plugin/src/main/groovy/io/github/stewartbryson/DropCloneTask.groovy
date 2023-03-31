package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.TaskAction

/**
 * A superclass for creating Gradle tasks that use ephemeral Snowflake clones.
 */
@Slf4j
abstract class DropCloneTask extends SnowflakeTask {

    DropCloneTask() {
        description = "A Cacheable Gradle task for dropping ephemeral testing environments in Snowflake."
        group = "verification"
    }

    /**
     * Drop the ephemeral Snowflake clone.
     */
    @TaskAction
    def dropClone() {
        // create the session
        createSession()
        // set the ephemeral name
        // we do not want to set the context
        snowflake.setEphemeral(extension.ephemeralName, false)
        snowflake.setOriginalContext()
        // drop the ephemeral database
        try {
            snowflake.ephemeral = extension.ephemeralName
            snowflake.session.jdbcConnection().createStatement().execute("drop database if exists ${snowflake.ephemeral}")
        } catch (Exception e) {
            throw new Exception("Dropping ephemeral clone failed.", e)
        }
        log.warn "Ephemeral clone $snowflake.ephemeral dropped."
    }
}
