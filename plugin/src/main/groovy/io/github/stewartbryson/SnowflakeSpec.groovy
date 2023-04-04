package io.github.stewartbryson

import spock.lang.Shared
import spock.lang.Specification

class SnowflakeSpec extends Specification {

   @Shared
   String connection = System.getProperty('connection')

   @Shared
   String ephemeral

   @Shared
   Snowflake snowflake

   def setupSpec() {
      ephemeral = System.getProperty('ephemeral')
      if (ephemeral) {
         snowflake.setEphemeral(ephemeral)
      }
      snowflake = new Snowflake(connection)
   }
}
