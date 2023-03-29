package io.github.stewartbryson

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j

import java.sql.ResultSet
import java.sql.Statement

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
     * The ephemeral Snowflake clone name.
     */
    String keepEphemeral

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
     * Constructor using auto-detected Snowsql config file.
     *
     * @return Snowflake class.
     */
    Snowflake(String connection) {
        this.snowConfig = new SnowConfig(connection)
        construct()
    }

    /**
     * Constructor using auto-detected Snowsql config file and an ephemeral clone.
     *
     * @return Snowflake class.
     */
    Snowflake(String connection, String ephemeral, Boolean keepEphemeral= false) {
        this.snowConfig = new SnowConfig(connection)
        this.ephemeral = ephemeral
        this.keepEphemeral = keepEphemeral
        construct()
    }

    /**
     * Constructor using explicit Snowsql config file as a File object.
     *
     * @return Snowflake class.
     */
    Snowflake(File config, String connection) {
        this.snowConfig = new SnowConfig(config, connection)
        construct()
    }

    /**
     * Constructor using explicit Snowsql config file as a File object.
     *
     * @return Snowflake class.
     */
    Snowflake(File config, String connection, String ephemeral, Boolean keepEphemeral = false) {
        this.snowConfig = new SnowConfig(config, connection)
        this.ephemeral = ephemeral
        this.keepEphemeral = keepEphemeral
        construct()
    }

    /**
     * Reusable construction functionality for multiple constructors.
     */
    private def construct() {
        Map props = snowConfig.getConnectionsProps()
        Map printable = props.clone()
        printable.password = "*********"
        log.warn "Snowflake config: $printable"

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
            log.debug "Connection database, schema, role: $connectionDatabase, $connectionSchema, $connectionRole"
        } catch (Exception e) {
            throw new Exception("Connection context is not available.", e)
        }

        // create the clone if ephemeral
        if (isEphemeral()) {
            createClone()
        }
    }

    /**
     * Create a snowflake ephemeral clone.
     */
    private def createClone() {
        // create the clone
        try {
            session.jdbcConnection().createStatement().execute("create database if not exists ${ephemeral} clone ${connectionDatabase}")
            session.jdbcConnection().createStatement().execute("grant ownership on database ${ephemeral} to ${connectionRole}")
            session.jdbcConnection().createStatement().execute("use database ${ephemeral}")
            session.jdbcConnection().createStatement().execute("use schema ${ephemeral}.${connectionSchema}")
        } catch (Exception e) {
            throw new Exception("Cloning ephemeral clone failed.", e)
        }
        log.warn "Ephemeral clone $ephemeral created."

    }

    /**
     * Drop the ephemeral Snowflake clone.
     */
    private def dropClone() {
        // drop the ephemeral database
        try {
            session.jdbcConnection().createStatement().execute("drop database if exists ${ephemeral}")
            session.jdbcConnection().createStatement().execute("use database ${connectionDatabase}")
            session.jdbcConnection().createStatement().execute("use schema ${connectionDatabase}.${connectionSchema}")
        } catch (Exception e) {
            throw new Exception("Dropping ephemeral clone failed.", e)
        }
        log.warn "Ephemeral clone $ephemeral dropped."

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