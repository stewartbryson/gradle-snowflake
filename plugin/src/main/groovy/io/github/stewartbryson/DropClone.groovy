package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task for dropping a Snowflake clone for ephemeral testing.
 */
@Slf4j
abstract class DropClone extends SnowflakeTask {

    /**
     * The task Constructor with 'description' and 'group'.
     *
     * @return A custom task class.
     */
    DropClone() {
        description = "A Gradle task for dropping a Snowflake clone for ephemeral testing."
        group = "verification"
    }

    /**
     * Override {@link SnowflakeTask#getRevisedDatabase} for creating the database clone.
     *
     * @return The database to connect to.
     */
    @Internal
    @Override
    def getRevisedDatabase() {
        database
    }

    /**
     * The Gradle TaskAction method. Drop the ephemeral database.
     */
    @TaskAction
    def dropClone() {
        // drop the database clone
        session.jdbcConnection().createStatement().execute("drop database if exists ${extension.ephemeralName}")

        // close the session
        session.close()
    }
}
