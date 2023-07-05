package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

@Slf4j
class SnowConfigTest extends Specification {

    @Shared
    SnowConfig snow

    @TempDir
    @Shared
    private File projectDir

    @Shared
    File config

    def setup() {
        config = new File(projectDir, "config")
        config.write("""
                |[connections]
                |accountname = defaultaccount
                |username = defaultuser
                |password = defaultpassword
                |dbname = defaultdbname
                |schemaname = defaultschema
                |warehousename = defaultwarehouse
                |rolename = defaultrolename
                |""".stripMargin())

        snow = new SnowConfig(config, "example")
    }

    def "Parse credentials default connection"() {
        when:
        def props = snow.getConnectionsProps()

        then:
        props.getClass() == LinkedHashMap<String, String>
        props.url == 'https://defaultaccount.snowflakecomputing.com'
        props.warehouse == 'defaultwarehouse'
        props.user == 'defaultuser'
        props.password == 'defaultpassword'
    }

    def "Parse credentials connection with quoted password"() {
        given:
        config.append("""
                |
                |[connections.example]
                |username = username
                |password = "password#1234"
                |""".stripMargin())
        when:
        def props = snow.getConnectionsProps()

        then:
        props.getClass() == LinkedHashMap<String, String>
        props.user == 'username'
        props.password == 'password#1234'
    }
}
