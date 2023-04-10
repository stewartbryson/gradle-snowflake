package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.ini4j.Ini

/**
 * A class for parsing a SnowSQL config file.
 */
@Slf4j
class SnowConfig {
    /**
     * The snowsql config file.
     */
    File config

    /**
     * The SnowSQL connection to use.
     */
    String connection

    /**
     * Constructor using auto-detected SnowSQL config file.
     *
     * @return Snowflake class.
     */
    SnowConfig(String connection) {
        this.connection = connection
        // first look for ~/.snowsql/config
        def homeConfig = new File(System.getProperty("user.home").toString() + "/.snowsql/config")
        // then look for ./snowconfig
        def projectConfig = new File('snow-config')
        if (homeConfig.exists()) {
            config = homeConfig
        } else if (projectConfig.exists()) {
            config = projectConfig
        } else {
            throw new Exception("Unable to find a SnowSQL config file.")
        }
        log.warn "Using snowsql config file: ${config.absolutePath}"
    }

    /**
     * Constructor using explicit SnowSQL config file as a File object.
     *
     * @return Snowflake class.
     */
    SnowConfig(File config, String connection) {
        this.connection = connection
        this.config = config
    }

    /**
     * Constructor using explicit SnowSQL config file as a String path.
     *
     * @return Snowflake class.
     */
    SnowConfig(String config, String connection) {
        this.connection = connection
        this.config = new File(config)
    }

    /**
     * Build a Map of connection properties for making a Snowflake connection.
     *
     * @return Snowflake connection properties.
     */
    Map getConnectionsProps() {
        //Map props1 = [account: "account", user: "user", password: "password"]
        Map props = [:]
        Ini ini = new Ini(config)
        // first get all the connection defaults
        ini.get("connections").each { key, value ->
            props."${key.replaceAll(/name$/, '')}" = value
        }
        // now replace defaults with the connection props
        String connectionName = "connections" + '.' + connection
        ini.get(connectionName).each { key, value ->
            props."${key.replaceAll(/name$/, '')}" = value
        }
        // construct url from account
        props.url = "https://" + props.account + ".snowflakecomputing.com"
        props.remove("account")
        // special password handling to support quoted values
        props.password = props.password.toString().replaceAll(/("*)([^"$]+)("*)/) {all, q1, pwd, q2 ->
            pwd
        }

        // we need at least these three to make a connection
        if (!props.url || !props.user || !props.password) {
            throw new Exception("'url', 'user', or 'password' is not configured.")
        }
        return props
    }
}