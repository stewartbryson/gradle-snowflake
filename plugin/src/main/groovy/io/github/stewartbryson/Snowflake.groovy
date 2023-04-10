package io.github.stewartbryson

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j

import java.sql.ResultSet
import java.sql.Statement

/**
 * A class that manages connecting to Snowflake, as well as providing information about that connection. Used by core plugin tasks as well as the {@link SnowflakeSpec} specification.
 */
@Slf4j
class Snowflake {
    /**
     * SnowConfig object.
     */
    SnowConfig snowConfig

    /**
     * The Snowpark session.
     */
    Session session

    /**
     * The ephemeral Snowflake clone name.
     */
    String ephemeral

    /**
     * Whether an ephemeral clone is in use.
     *
     * @return whether an ephemeral clone is in use.
     */
    Boolean isEphemeral() {
        (ephemeral ? true : false)
    }

    /**
     * Determines whether a session exists.
     *
     * @return whether a session exists.
     */
    Boolean hasSession() {
        //return (session ? true : false)
        if (!session) {
            return false
        } else if (session && getScalarValue("SELECT 1")) {
            return true
        } else {
            return false
        }
    }

    /**
     * The connection database in case it wasn't in the connection.
     */
    String connectionDatabase

    /**
     * The connection schema in case it wasn't in the connection.
     */
    String connectionSchema

    /**
     * The connection role in case it wasn't in the connection.
     */
    String connectionRole

    /**
     * Empty constructor for use in the Gradle project.
     *
     * @return Empty Snowflake class.
     */
    Snowflake() {}

    /**
     * Constructor using autodetected SnowSQL config file.
     *
     * @return Snowflake class.
     */
    Snowflake(String connection) {
        this.snowConfig = new SnowConfig(connection)
        construct()
    }

    /**
     * Constructor using explicit SnowSQL config file as a File object.
     *
     * @return Snowflake class.
     */
    Snowflake(File config, String connection) {
        this.snowConfig = new SnowConfig(config, connection)
        construct()
    }

    /**
     * Reusable construction functionality for multiple constructors.
     */
    private def construct() {
        Map props = snowConfig.getConnectionsProps()
        Map printable = props.clone()
        printable.password = "*********"
        log.info "Snowflake config: $printable"

        // get a Snowflake session
        try {
            session = Session.builder().configs(props).create()
            session.jdbcConnection().createStatement().execute("ALTER SESSION SET JDBC_QUERY_RESULT_FORMAT='JSON'")
        } catch (NullPointerException npe) {
            throw new Exception("Snowflake connection details are missing.", npe)
        }

        // record current session values
        try {
            connectionDatabase = getScalarValue('SELECT CURRENT_DATABASE()')
            connectionSchema = getScalarValue('SELECT CURRENT_SCHEMA()')
            connectionRole = getScalarValue('SELECT CURRENT_ROLE()')
            log.info "Connection database, schema, role: $connectionDatabase, $connectionSchema, $connectionRole"
        } catch (Exception e) {
            throw new Exception("Connection context is not available.", e)
        }
    }

    /**
     * Set original connection context in the Snowflake session.
     */
    def setOriginalContext() {
        session.jdbcConnection().createStatement().execute("use database ${connectionDatabase}")
        session.jdbcConnection().createStatement().execute("use schema ${connectionDatabase}.${connectionSchema}")
    }

    /**
     * Set ephemeral context in the Snowflake session.
     */
    def setEphemeralContext() {
        session.jdbcConnection().createStatement().execute("use database ${ephemeral}")
        session.jdbcConnection().createStatement().execute("use schema ${ephemeral}.${connectionSchema}")
    }

    /**
     * Return the first column from the first row of a SELECT statement.
     *
     * @return a scalar column value.
     */
    def getScalarValue(String sql) {
        Statement statement = session.jdbcConnection().createStatement()
        ResultSet rs = statement.executeQuery(sql)
        def columnValue
        if (rs.next()) {
            columnValue = rs.getString(1)
        }
        // ensure we are matching our stage with our url
        rs.close()
        statement.close()
        return columnValue
    }
}