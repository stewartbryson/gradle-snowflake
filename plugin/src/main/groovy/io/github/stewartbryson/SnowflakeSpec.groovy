package io.github.stewartbryson

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class SnowflakeSpec extends Specification {

   @Shared
   String connection = System.getProperty('connection')

   @Shared
   String ephemeral = System.getProperty('ephemeralName')

   @Shared
   @Subject
   Snowflake snowflake

   def setupSpec() {
      snowflake = new Snowflake(connection)
      snowflake.setEphemeral(ephemeral)
   }
}
