package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class SnowflakeTest extends Specification {

    @Shared
    Snowflake snowflake

    def "parse credentials config"() {
        given: "a Snowflake class"
        snowflake = new Snowflake('gradle_plugin')

        expect: "SELECT statements are successful"
        snowflake.getScalarValue("SELECT 1").toInteger() == 1
    }

    def "isEphemeral() function"() {
        given: "a Snowflake class with ephemeralName updated"
        snowflake = new Snowflake('gradle_plugin')
        snowflake.ephemeralName = 'functional_test'

        expect: "Boolean function works"
        snowflake.isUsingEphemeral()
    }

    def "hasSession() function is false"() {
        given: "an empty Snowflake class"
        snowflake = new Snowflake()

        expect: "Boolean function works"
        !snowflake.hasSession()
    }

    def "hasSession() function is true"() {
        given: "an non-empty Snowflake class"
        snowflake = new Snowflake('gradle_plugin')

        expect: "Boolean function works"
        snowflake.hasSession()
    }
}
