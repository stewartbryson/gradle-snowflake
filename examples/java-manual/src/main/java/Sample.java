public class Sample {

  public String addNum(int num1, int num2) {
    try {
      int sum = num1 + num2;
      return ("Sum is: " + sum);
    } catch (Exception e) {
      return null;
    }
  }

  public static void main(String[] args) {
    System.out.println("Hello World");
  }
}