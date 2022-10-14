package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*

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
     * The Gradle TaskAction method. Create the ephemeral clone.
     */
    @TaskAction
    def createClone() {
        // create the database clone
        String database = getColumnValue("select current_database()")
        String role = getColumnValue("select current_role()")
        session.jdbcConnection().createStatement().execute("create or replace database ${extension.cloneName} clone $database")
        session.jdbcConnection().createStatement().execute("grant ownership on database ${extension.cloneName} to $role")
        session.jdbcConnection().createStatement().execute("use database ${extension.cloneName}")

        // close the session
        session.close()
    }
}
