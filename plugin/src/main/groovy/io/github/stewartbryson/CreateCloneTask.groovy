package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task for creating ephemeral testing environments in Snowflake.
 */
@Slf4j
abstract class CreateCloneTask extends SnowflakeTask {

    /**
     * Constructor.
     */
    CreateCloneTask() {
        description = "A Gradle task for creating ephemeral testing environments in Snowflake."
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
        snowflake.ephemeralName = extension.ephemeralName
        try {
            snowflake.session.jdbcConnection().createStatement().execute("create database if not exists ${snowflake.ephemeralName} clone ${snowflake.connectionDatabase}")
            snowflake.session.jdbcConnection().createStatement().execute("grant ownership on database ${snowflake.ephemeralName} to ${snowflake.connectionRole}")
        } catch (Exception e) {
            throw new Exception("Creating ephemeral clone failed.", e)
        }
        log.warn "Ephemeral clone ${snowflake.ephemeralName} created if not exists."

        // Until we build a better testing spec, let downstream dependencies set this
        //snowflake.setEphemeralContext()
    }
}
