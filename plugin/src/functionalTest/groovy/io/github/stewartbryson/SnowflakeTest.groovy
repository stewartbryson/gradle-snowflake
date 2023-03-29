package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class SnowflakeTest extends Specification {

    @Shared
    Snowflake snowflake

    def "parse snowsql config"() {
        given:
        snowflake = new Snowflake('gradle_plugin')

        when:
        def sql = 'SELECT 1'

        then:
        snowflake.getScalarValue(sql).toInteger() == 1
    }

    def "isEphemeral() function"() {
        given:
        snowflake = new Snowflake('gradle_plugin')

        when:
        snowflake.ephemeral = 'functional_test'

        then:
        snowflake.isEphemeral()
    }

    def "creating ephemeral clone"() {
        when:
        snowflake = new Snowflake('gradle_plugin', 'functional_test')

        then:
        snowflake.isEphemeral()
        snowflake.getScalarValue("select CURRENT_DATABASE()") == snowflake.ephemeral.toUpperCase()
    }
}
