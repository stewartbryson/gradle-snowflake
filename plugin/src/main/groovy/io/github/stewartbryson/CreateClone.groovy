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
     * The Snowflake database to connect to. Overrides {@link SnowflakeExtension#database}.
     */
    @Optional
    @Input
    @Option(option = "database",
            description = "Override the Snowflake database to connect to."
    )
    // should always connect to the database specified
    String database = extension.database

    /**
     * The Gradle TaskAction method. Create the ephemeral clone.
     */
    @TaskAction
    def createClone() {
        // create the database clone
        String database = getColumnValue("select current_database()")
        String role = getColumnValue("select current_role()")
        session.jdbcConnection().createStatement().execute("create or replace database ${extension.ephemeralName} clone $database")
        session.jdbcConnection().createStatement().execute("grant ownership on database ${extension.ephemeralName} to $role")
        session.jdbcConnection().createStatement().execute("use database ${extension.ephemeralName}")

        // close the session
        session.close()
    }
}
