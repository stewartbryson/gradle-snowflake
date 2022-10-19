package io.github.stewartbryson

import com.snowflake.snowpark_java.PutResult
import groovy.util.logging.Slf4j
import net.snowflake.client.jdbc.SnowflakeStatement
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
abstract class SnowflakePublish extends SnowflakeEphemeralTask {

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
        // create the clone and then store the session to the clone
        //todo Make this automatic as part of SnowflakeEphemeralTask
        createClone()

        String jar = project.tasks.shadowJar.archiveFile.get()

        if (!extension.publishUrl && !extension.useCustomMaven) {
            // create the internal stage if it doesn't exist
            session.jdbcConnection().createStatement().execute("create stage if not exists ${stage}")

            //
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
            String selectStage = getColumnValue("select stage_url from information_schema.stages where stage_name=upper('$stage') and stage_schema=upper('$schema') and stage_type='External Named'")
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
        // drop the clone
        //todo Make this automatic as part of SnowflakeEphemeralTask
        dropClone()

        // close the session
        session.close()
    }
}
