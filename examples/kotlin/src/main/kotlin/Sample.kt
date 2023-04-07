class Sample {
  fun addNum(num1: Int, num2: Int): String {
    try {
      return "Sum is: " + (num1 + num2).toString()
    } catch (e: Exception) {
      return null.toString()
    }
  }
}
