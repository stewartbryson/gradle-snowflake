import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AddNumbers {

  private static final Logger logger = LogManager.getLogger();

  public String addNum(int num1, int num2) {
    try {
      int sum = num1 + num2;
      return ("Sum is: " + sum);
    } catch (Exception e) {
      logger.warn("Error: " + e.toString());
      return null;
    }
  }

  public static void main(String[] args) {
    System.out.println("Hello World");
  }
}
