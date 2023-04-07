package io.github.stewartbryson

import spock.lang.Shared
import spock.lang.Specification

class SnowflakeSpec extends Specification {

   @Shared
   private String connection = System.getProperty('connection')

   @Shared
   private String ephemeral = System.getProperty('ephemeral')

   @Shared
   private Snowflake snowflake = new Snowflake()

   def setupSpec() {
      snowflake = new Snowflake(connection)
      if (ephemeral) {
         snowflake.ephemeral = ephemeral
         snowflake.setEphemeralContext()
      }
   }

   def selectSingleValue(String sql) {
      snowflake.getScalarValue(sql)
   }
}
