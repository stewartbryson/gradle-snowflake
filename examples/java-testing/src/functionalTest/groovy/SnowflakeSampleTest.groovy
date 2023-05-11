import groovy.util.logging.Slf4j
import io.github.stewartbryson.SnowflakeSpec

/**
* The SnowflakeSpec used for testing functions.
*/
@Slf4j
class SnowflakeSampleTest extends SnowflakeSpec {

    def 'ADD_NUMBERS() function with 1 and 2'() {
        when: "Two numbers exist"
        def a = 1
        def b = 2

        then: 'Add two numbers using ADD_NUMBERS()'
        selectFunction("add_numbers", [a,b]) == 'Sum is: 3'
    }

    def 'ADD_NUMBERS() function with 3 and 4'() {
        when: "Two numbers exist"
        def a = 3
        def b = 4

        then: 'Add two numbers using ADD_NUMBERS()'
        selectFunction("add_numbers", [a,b]) == 'Sum is: 7'
    }

}
