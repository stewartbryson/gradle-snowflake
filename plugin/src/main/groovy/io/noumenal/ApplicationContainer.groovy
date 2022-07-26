package io.noumenal

import groovy.util.logging.Slf4j

@Slf4j
class ApplicationContainer {
   ApplicationContainer(final String name) {
      this.name = name
   }
   String name
   List inputs
   String returns
   String handler
   Boolean hasReplace = true

   String getObjectType() {
      returns ? 'function' : 'procedure'
   }

   Boolean isFunction() {
      objectType == 'function'
   }

   String getCreate() {
      "CREATE ${hasReplace ? 'OR REPLACE ' : ''} $objectType $name (${inputs.join(', ')})\n" +
              (isFunction() ? "  returns ${returns}\n" : "") +
              """|  language JAVA
                 |  handler = '$handler'
                 |  """.stripMargin()
   }

}
