package io.noumenal

import groovy.util.logging.Slf4j

/**
 * A domain container that allows for defining "Snowflake Applications." The plugin automatically creates the UDFs configured in this container.
 */
@Slf4j
class ApplicationContainer {
   ApplicationContainer(final String name) {
      this.name = name
   }
   /**
    * The name of the domain container.
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
    * The 'language' property of the UDF, currently only 'JAVA' is supported. DEFAULT: 'JAVA'.
    */
   String language = 'JAVA'
   /**
    * The 'handler' property of the UDF.
    */
   String handler

   /**
    * A getter for the create statement for the UDF. The imports are passed in as the only property.
    *
    * @return The complete UDF create statement.
    */
   String getCreate(String imports) {
      """|CREATE OR REPLACE $type $name (${inputs.join(', ')})
         |  returns $returns
         |  language $language
         |  handler = '$handler'
         |  imports = ($imports)
         |""".stripMargin()
   }
}
