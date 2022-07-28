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
   String type = 'function'
   String language = 'JAVA'
   String handler
   Boolean hasReplace = true

   String getObjectType() {
      returns ? 'function' : 'procedure'
   }

   Boolean isFunction() {
      objectType == 'function'
   }

   String getCreate(String imports) {
      """|CREATE OR REPLACE $type $name (${inputs.join(', ')})
         |  returns $returns
         |  language $language
         |  handler = '$handler'
         |  imports = ($imports)
         |""".stripMargin()
   }
}
