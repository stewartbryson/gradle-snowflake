package io.github.stewartbryson

import groovy.util.logging.Slf4j

/**
 * A domain container that allows for defining "Snowflake Applications." The plugin automatically creates the UDFs and procedures configured in this container.
 */
@Slf4j
class ApplicationContainer {
   ApplicationContainer(final String name) {
      this.name = name
   }
   /**
    * The name of the domain container, which equates to the name of the function or procedure.
    */
   String name
   /**
    * The 'inputs' property for the Snowflake UDF.
    */
   List inputs
   /**
    * The 'returns' property for the Snowflake UDF.
    */
   String returns
   /**
    * The 'type' property of the UDF, either 'function' or 'procedure'. DEFAULT: 'function'.
    */
   String type = 'function'
   /**
    * The 'language' property of the UDF. DEFAULT: 'JAVA'.
    */
   String language = 'JAVA'
   /**
    * The 'handler' property of the UDF.
    */
   String handler
   /**
    * Is the UDF immutable? DEFAULT: false
    */
   Boolean immutable = false

   /**
    * A getter for the create statement for the UDF. The imports are passed in as the only property.
    *
    * @return The complete UDF create statement.
    */
   String getCreate(String imports) {
      """|CREATE OR REPLACE $type $name (${inputs.join(', ')})
         |  returns $returns
         |  language $language ${immutable ? "\n  IMMUTABLE" : ""}
         |  handler = '$handler'
         |  imports = ($imports)
         |""".stripMargin()
   }
}
