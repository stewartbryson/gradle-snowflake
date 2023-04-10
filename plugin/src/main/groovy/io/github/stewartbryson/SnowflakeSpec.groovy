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
   private String ephemeral = System.getProperty('ephemeral')

   @Shared
   private Snowflake snowflake = new Snowflake()

   /**
    * Built-in Spock method executed at the beginning of spec execution.
    */
   def setupSpec() {
      snowflake = new Snowflake(connection)
      if (ephemeral) {
         snowflake.ephemeral = ephemeral
         snowflake.setEphemeralContext()
      }
   }

   /**
    * Returns the first column of the first row of a SELECT statement. Useful for testing scalar function calls.
    * @param sql
    * @return The first column of the first row of a SELECT statement.
    */
   def selectSingleValue(String sql) {
      snowflake.getScalarValue(sql)
   }
}
