package vyxal

object NumberHelpers {

  def multiplicity(a: VNum, b: VNum): VNum = {
    var result = 0
    var current = a
    while (current % b == 0) {
      result += 1
      current /= b
    }
    result
  }

  def range(a: VNum, b: VNum): VList = {
    val start = a.toInt
    val end = b.toInt
    val step = if (start < end) 1 else -1

    VList((start to end by step).map(VNum(_))*)
  }
}
