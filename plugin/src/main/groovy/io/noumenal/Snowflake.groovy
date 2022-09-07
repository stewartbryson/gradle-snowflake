package io.noumenal

import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j
import net.snowflake.client.jdbc.SnowflakeStatement

import java.sql.ResultSet
import java.sql.Statement

@Slf4j
class Snowflake {
    Map  props
    Session session

    Snowflake(Map props) {
        this.props = props
        Map printable = props.clone()
        printable.password = "*********"
        log.info "Snowflake config: $printable"
        try {
            this.session = Session.builder().configs(this.props).create()
        } catch (NullPointerException npe) {
            throw new Exception("Snowflake connection details are missing.", npe)
        }
    }

    def assertStage(String stage, String schema, String publishUrl) {
        Statement statement = session.jdbcConnection().createStatement()
        String sql = "select stage_url from information_schema.stages where stage_name=upper('$stage') and stage_schema=upper('$schema') and stage_type='External Named'"
        ResultSet rs = statement.executeQuery(sql)
        String selectStage
        if (rs.next()) {
            selectStage = rs.getString(1)
        }
        rs.close()
        statement.close()
        // ensure we are matching our stage with our url
        selectStage == publishUrl
    }
}
