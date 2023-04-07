import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class SampleTest extends Specification {
   @Shared
   @Subject
   def sample = new Sample()

   def "adding 1 and 2"() {
      when: "Two numbers"
      def one = 1
      def two = 2
      then: "Add numbers"
      sample.addNum(one, two) == "Sum is: 3"
   }

   def "adding 3 and 4"() {
      when: "Two numbers"
      def one = 3
      def two = 4
      then: "Add numbers"
      sample.addNum(one, two) == "Sum is: 7"
   }
}
