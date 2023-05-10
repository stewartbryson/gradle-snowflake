import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class SampleTest extends Specification {
   @Shared
   @Subject
   def sample = new Sample()

   def "adding 1 and 2"() {
      when: "Two numbers"
      def a = 1
      def b = 2
      then: "Add numbers"
      sample.addNum(a, b) == "Sum is: 3"
   }

   def "adding 3 and 4"() {
      when: "Two numbers"
      def a = 3
      def b = 4
      then: "Add numbers"
      sample.addNum(a, b) == "Sum is: 7"
   }
}
