package io.github.stewartbryson

import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification

/**
 * A Spock specification for functional testing in Snowflake.
 */
@Slf4j
class SnowflakeSpec extends Specification {

   @Shared
   private String connection = System.getProperty('connection')

   @Shared
   private String ephemeralName = System.getProperty('ephemeralName')

   @Shared
   private Snowflake snowflake = new Snowflake()

   /**
    * Built-in Spock method executed at the beginning of spec execution.
    */
   def setupSpec() {
      snowflake = new Snowflake(connection)
      if (ephemeralName) {
         snowflake.ephemeralName = ephemeralName
         snowflake.setEphemeralContext()
      }
   }

   /**
    * Returns the first column of the first row of a SELECT statement.
    * @param sql The SQL statement to execute.
    * @return The first column of the first row of a SELECT statement.
    */
   def selectSingleValue(String sql) {
      snowflake.getScalarValue(sql)
   }

   /**
    * Returns the the result of a scalar function call.
    * @param name The name of the function.
    * @param arguments A list of arguments to pass to the function.
    * @return The first column of the first row of a SELECT statement.
    */
   def selectFunction(String name, List arguments) {
      String sql = "select $name(${arguments.join(",")});"
      log.warn "Function SQL: $sql"
      selectSingleValue(sql)
   }
}
