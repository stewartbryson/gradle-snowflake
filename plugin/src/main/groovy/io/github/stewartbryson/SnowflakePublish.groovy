package io.github.stewartbryson

import com.snowflake.snowpark_java.PutResult
import com.snowflake.snowpark_java.Session
import groovy.util.logging.Slf4j
import net.snowflake.client.jdbc.SnowflakeStatement
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.sql.ResultSet
import java.sql.Statement

/**
 * A Cacheable Gradle task for publishing Java-based applications as UDFs to Snowflake.
 */
@Slf4j
@CacheableTask
abstract class SnowflakePublish extends DefaultTask {

    private static String PLUGIN = 'snowflake'

    /**
     * A helper for getting the plugin extension.
     *
     * @return A reference to the plugin extension.
     */
    @Internal
    def getExtension() {
        project.extensions."$PLUGIN"
    }

    /**
     * The task Constructor with 'description' and 'group'.
     *
     * @return A custom task class.
     */
    SnowflakePublish() {
        description = "A Cacheable Gradle task for publishing Java-based applications as UDFs to Snowflake."
        group = "publishing"
    }

    /**
     * The Snowflake account URL, for instance: https://gradle-snowflake.us-east-1.snowflakecomputing.com:443. Overrides {@link SnowflakeExtension#account}.
     */
    @Optional
    @Input
    @Option(option = "account",
            description = "Override the URL of the Snowflake account."
    )
    String account = extension.account

    /**
     * The Snowflake user to connect as. Overrides {@link SnowflakeExtension#user}.
     */
    @Optional
    @Input
    @Option(option = "user",
            description = "Override the Snowflake user to connect as."
    )
    String user = extension.user

    /**
     * The Snowflake password to connect with. Overrides {@link SnowflakeExtension#password}.
     */
    @Optional
    @Input
    @Option(option = "password",
            description = "Override the Snowflake password to connect with."
    )
    String password = extension.password

    /**
     * The Snowflake database to connect to. Overrides {@link SnowflakeExtension#database}.
     */
    @Optional
    @Input
    @Option(option = "database",
            description = "Override the Snowflake database to connect to."
    )
    String database = extension.database

    /**
     * The Snowflake schema to connect with. Overrides {@link SnowflakeExtension#schema}.
     */
    @Input
    @Option(option = "schema",
            description = "Override the Snowflake schema to connect with."
    )
    String schema = extension.schema

    /**
     * The Snowflake warehouse to use. Overrides {@link SnowflakeExtension#warehouse}.
     */
    @Input
    @Option(option = "warehouse",
            description = "Override the Snowflake warehouse to use."
    )
    String warehouse = extension.warehouse

    /**
     * The Snowflake role to connect with. Overrides {@link SnowflakeExtension#role}.
     */
    @Input
    @Option(option = "role",
            description = "Override the Snowflake role to connect with."
    )
    String role = extension.role

    /**
     * The Snowflake stage to publish to. Overrides {@link SnowflakeExtension#stage}.
     */
    @Optional
    @Input
    @Option(option = "stage",
            description = "Override the Snowflake stage to publish to."
    )
    String stage = extension.stage

    /**
     * Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.
     */
    @Optional
    @Input
    @Option(option = "jar", description = "Optional: manually pass a JAR file path to upload instead of relying on Gradle metadata.")
    String jar = project.tasks.shadowJar.archiveFile.get()

    /**
     * Create a Snowflake session.
     *
     * @return a Snowflake session.
     */
    @Internal
    Session getSession() {
        Map props = [
                url      : account,
                user     : user,
                password : password,
                role     : role,
                warehouse: warehouse,
                db       : database,
                schema   : schema
        ]
        Map printable = props.clone()
        printable.password = "*********"
        log.info "Snowflake config: $printable"

        Session session
        // get a Snowflake session
        try {
            session = Session.builder().configs(props).create()
        } catch (NullPointerException npe) {
            throw new Exception("Snowflake connection details are missing.", npe)
        }
        return session
    }

    /**
     * A simple text output file for the Snowflake applications create statements. Mainly for making the class Cacheable.
     */
    @OutputFile
    File output = project.file("${project.buildDir}/${PLUGIN}/output.txt")

    /**
     * Get the 'import' property for the UDF.
     *
     * @return the 'import' property.
     */
    @Internal
    String getImports() {

        String basePath = "@${stage}/${extension.groupId.replace('.', '/')}/${extension.artifactId}/${project.version}"
        //log.warn "basePath: $basePath"
        Statement statement = session.jdbcConnection().createStatement()
        String sql = "LIST $basePath pattern='(.)*(-all)\\.jar'; select * from table(result_scan(last_query_id())) order by 'last_modified' asc;"
        statement.unwrap(SnowflakeStatement.class).setParameter(
                "MULTI_STATEMENT_COUNT", 2)
        ResultSet rs = statement.executeQuery(sql)
        String fileName
        String filePath
        try {
            while (rs.next()) {
                filePath = rs.getString(1)
            }
            fileName = filePath.replaceAll(/(.*)($project.version)(\/)(.*)/) { all, first, version, slash, filename ->
                filename
            }
        } catch (Exception e) {
            throw new Exception("Unable to detect the correct JAR in stage '${stage}'.")
        }
        rs.close()
        statement.close()
        "'$basePath/$fileName'"
    }

    /**
     * The Gradle TaskAction method. Publish the Snowflake Application.
     */
    @TaskAction
    def publish() {
        // keep the session
        //Session session = this.session
        String jar = project.tasks.shadowJar.archiveFile.get()

        if (!extension.publishUrl && !extension.useCustomMaven) {
            def options = [
                    AUTO_COMPRESS: 'FALSE',
                    PARALLEL     : '4',
                    OVERWRITE    : 'TRUE'
            ]
            PutResult[] pr = session.file().put(jar, "$stage/libs", options)
            pr.each {
                log.warn "File ${it.sourceFileName}: ${it.status}"
            }
        } else if (extension.publishUrl) {
            // ensure that the stage and the publishUrl are aligned
            Statement statement = session.jdbcConnection().createStatement()
            String sql = "select stage_url from information_schema.stages where stage_name=upper('$stage') and stage_schema=upper('$schema') and stage_type='External Named'"
            ResultSet rs = statement.executeQuery(sql)
            String selectStage
            if (rs.next()) {
                selectStage = rs.getString(1)
            }
            // ensure we are matching our stage with our url
            rs.close()
            statement.close()
            assert selectStage == extension.publishUrl
        }

        // automatically create application spec objects
        output.write("Snowflake Application:\n\n")
        project."$PLUGIN".applications.each { ApplicationContainer app ->
            File jarFile = project.file(jar)
            String createText = app.getCreate(extension.publishUrl ? getImports() : "'@$stage/libs/${jarFile.name}'")
            String message = "Deploying ==> \n$createText"
            log.warn message
            output.append("$message\n")
            session.jdbcConnection().createStatement().execute(createText)
        }
        session.close()
    }
}
