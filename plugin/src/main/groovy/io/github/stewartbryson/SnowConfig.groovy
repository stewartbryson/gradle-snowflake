package io.github.stewartbryson

import groovy.util.logging.Slf4j
import org.ini4j.Ini

@Slf4j
class SnowConfig {
    /**
     * The snowsql config file.
     */
    File config

    /**
     * The Snowsql connection to use.
     */
    String connection

    /**
     * Constructor using auto-detected Snowsql config file.
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
            throw new Exception("Unable to find a Snowsql config file.")
        }
    }

    /**
     * Constructor using explicit Snowsql config file as a File object.
     *
     * @return Snowflake class.
     */
    SnowConfig(File config, String connection) {
        this.connection = connection
        this.config = config
    }

    /**
     * Constructor using explicit Snowsql config file as a String path.
     *
     * @return Snowflake class.
     */
    SnowConfig(String config, String connection) {
        this.connection = connection
        this.config = new File(config)
    }

    Map getConnectionsProps() {
        Map props1 = [account: "account", user: "user", password: "password"]
        Map props = [:]
        Ini ini = new Ini(config)
        // first get all the connection defaults
        ini.get("connections").each { key, value ->
            props."${key.replaceAll(/name$/, '')}" = value
        }
        // now replace defaults with the connection props
        ini.get("connections" + '.' + connection).each { key, value ->
            props."${key.replaceAll(/name$/, '')}" = value
        }
        // add the url to the end of account
        props.url = "https://" + props.account + ".snowflakecomputing.com"
        props.remove("account")

        // we need at least these three to make a connection
        assert props.url
        assert props.user
        assert props.password
        return props
    }
}