class Sample {
  def addNum(num1: Integer, num2: Integer): String = {
    try {
      "The sum is: " + (num1 + num2).toString()
    } catch {
      case e: Exception => return null
    }
  }
}
