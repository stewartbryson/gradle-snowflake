import groovy.util.logging.Slf4j
import io.github.stewartbryson.SnowflakeSpec

/**
* The SnowflakeSpec used for testing functions.
*/
@Slf4j
class SnowflakeSampleTest extends SnowflakeSpec {

    def 'ADD_NUMBERS() function with 1 and 2'() {
        when: "Two numbers exist"
        def one = 1
        def two = 2

        then: 'Add two numbers using ADD_NUMBERS()'
        selectSingleValue("select add_numbers($one,$two);") == 'Sum is: 3'
    }

    def 'ADD_NUMBERS() function with 3 and 4'() {
        when: "Two numbers exist"
        def three = 3
        def four = 4

        then: 'Add two numbers using ADD_NUMBERS()'
        selectSingleValue("select add_numbers($three,$four);") == 'Sum is: 7'
    }

}
