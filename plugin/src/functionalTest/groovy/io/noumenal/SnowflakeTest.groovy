package io.noumenal

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class SnowflakeTest extends Specification {
    @Shared
    String account = System.getProperty("snowflake.account"),
           user = System.getProperty("snowflake.user"),
           password = System.getProperty("snowflake.password"),
           publishUrl = System.getProperty("snowflake.publishUrl"),
           role = System.getProperty("snowflake.role"),
           database = System.getProperty("snowflake.database"),
           schema = System.getProperty("snowflake.schema"),
           stage = System.getProperty("snowflake.stage"),
           warehouse = System.getProperty("snowflake.warehouse")

    def "snowflake connection"() {
        when:
        def snowflake = new Snowflake([
                url      : account,
                user     : user,
                password : password,
                role     : role,
                warehouse: warehouse,
                db       : database,
                schema   : schema
        ])

        then:
        snowflake.props
        snowflake.session
        snowflake.assertStage(stage, schema, publishUrl)
    }
}
