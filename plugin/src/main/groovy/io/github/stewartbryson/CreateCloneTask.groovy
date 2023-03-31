package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

/**
 * A superclass for creating Gradle tasks that use ephemeral Snowflake clones.
 */
@Slf4j
abstract class CreateCloneTask extends SnowflakeTask {

    CreateCloneTask() {
        description = "A Cacheable Gradle task for creating ephemeral testing environments in Snowflake."
        group = "verification"
    }

    /**
     * Create an ephemeral Snowflake clone.
     */
    @TaskAction
    def createClone() {
        // create the session
        createSession()
        // set the ephemeral name
        // we do not want to change the context
        snowflake.setEphemeral(extension.ephemeralName, false)
        try {
            snowflake.session.jdbcConnection().createStatement().execute("create database if not exists ${snowflake.ephemeral} clone ${snowflake.connectionDatabase}")
            snowflake.session.jdbcConnection().createStatement().execute("grant ownership on database ${snowflake.ephemeral} to ${snowflake.connectionRole}")
        } catch (Exception e) {
            throw new Exception("Creating ephemeral clone failed.", e)
        }
        log.warn "Ephemeral clone ${snowflake.ephemeral} created."
    }
}
