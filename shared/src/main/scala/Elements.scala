package vyxal.impls
// todo figure out a better solution than putting this in a different package
// it's in a different package so that ElementTests can access the impls without
// other classes being able to access them

import scala.language.implicitConversions

import vyxal.*

given Conversion[Boolean, VNum] with
  def apply(s: Boolean): VNum = if s then 1 else 0

/** Implementations for elements */
case class Element(
    symbol: String,
    name: String,
    keywords: Seq[String],
    arity: Option[Int],
    vectorises: Boolean,
    overloads: Seq[String],
    impl: DirectFn
)

case class UnimplementedOverloadException(element: String, args: Any)
    extends RuntimeException(s"$element not supported for inputs $args")

object Elements {
  val elements: Map[String, Element] = Impls.elements.toMap

  private[impls] object Impls {
    val elements = collection.mutable.Map.empty[String, Element]

    /** Take a monad, dyad, or triad, and return a proper (not Partial) function
      * that errors if it's not defined for the input
      */
    def errorIfUndefined[T](
        name: String,
        fn: PartialFunction[T, Context ?=> VAny]
    ): T => Context ?=> VAny = args =>
      if (fn.isDefinedAt(args)) fn(args)
      else throw UnimplementedOverloadException(name, args)

    def addNilad(
        symbol: String,
        name: String,
        keywords: Seq[String],
        desc: String
    )(impl: Context ?=> VAny): Unit = {
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        Some(0),
        false,
        List(s"-> $desc"),
        () => ctx ?=> ctx.push(impl(using ctx))
      )
    }

    def addMonadHelper(
        symbol: String,
        name: String,
        keywords: Seq[String],
        vectorises: Boolean,
        overloads: Seq[String],
        impl: Monad
    ): Monad = {
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        Some(1),
        vectorises,
        overloads,
        { () => ctx ?=>
          ctx.push(impl(ctx.pop()))
        }
      )
      impl
    }

    def addMonad(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialFunction[VAny, Context ?=> VAny]): Monad =
      addMonadHelper(
        symbol,
        name,
        keywords,
        false,
        overloads,
        { arg =>
          // need to specially implement this because it doesn't take a tuple
          if (impl.isDefinedAt(arg)) impl(arg)
          else throw UnimplementedOverloadException(symbol, arg)
        }
      )

    def addMonadVect(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialFunction[VAny, Context ?=> VAny]): Monad =
      addMonadHelper(
        symbol,
        name,
        keywords,
        true,
        overloads,
        vect1 { arg =>
          // need to specially implement this because it doesn't take a tuple
          if (impl.isDefinedAt(arg)) impl(arg)
          else throw UnimplementedOverloadException(symbol, arg)
        }
      )

    def addDyadHelper(
        symbol: String,
        name: String,
        keywords: Seq[String],
        vectorises: Boolean,
        overloads: Seq[String],
        impl: VyFn[2]
    ): Dyad = {
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        Some(2),
        vectorises,
        overloads,
        { () => ctx ?=>
          val arg2, arg1 = ctx.pop()
          val args = (arg1, arg2)
          ctx.push(impl(args))
        }
      )
      (a, b) => impl((a, b))
    }

    def addDyad(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialVyFn[2]): Dyad =
      addDyadHelper(
        symbol,
        name,
        keywords,
        false,
        overloads,
        errorIfUndefined(symbol, impl)
      )

    def addDyadVect(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialVyFn[2]) =
      addDyadHelper(
        symbol,
        name,
        keywords,
        true,
        overloads,
        vect2(errorIfUndefined(symbol, impl))
      )

    def addTriadHelper(
        symbol: String,
        name: String,
        keywords: Seq[String],
        vectorises: Boolean,
        overloads: Seq[String],
        impl: VyFn[3]
    ): Triad = {
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        Some(3),
        vectorises,
        overloads,
        { () => ctx ?=>
          val arg3, arg2, arg1 = ctx.pop()
          val args = (arg1, arg2, arg3)
          ctx.push(impl(args))
        }
      )
      (a, b, c) => impl((a, b, c))
    }

    def addTriad(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialVyFn[3]): Triad = addTriadHelper(
      symbol,
      name,
      keywords,
      false,
      overloads,
      errorIfUndefined(name, impl)
    )

    def addTriadVect(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialVyFn[3]) =
      addTriadHelper(
        symbol,
        name,
        keywords,
        true,
        overloads,
        vect3(errorIfUndefined(symbol, impl))
      )

    def addTetradHelper(
        symbol: String,
        name: String,
        keywords: Seq[String],
        vectorises: Boolean,
        overloads: Seq[String],
        impl: VyFn[4]
    ): Tetrad = {
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        Some(4),
        vectorises,
        overloads,
        { () => ctx ?=>
          val arg4, arg3, arg2, arg1 = ctx.pop()
          val args = (arg1, arg2, arg3, arg4)
          ctx.push(impl(args))
        }
      )
      (a, b, c, d) => impl((a, b, c, d))
    }

    def addTetrad(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: PartialVyFn[4]): Tetrad = addTetradHelper(
      symbol,
      name,
      keywords,
      false,
      overloads,
      errorIfUndefined(symbol, impl)
    )

    /** Add an element that works directly on the entire stack */
    def addDirect(
        symbol: String,
        name: String,
        keywords: Seq[String],
        overloads: String*
    )(impl: Context ?=> Unit): Unit =
      elements += symbol -> Element(
        symbol,
        name,
        keywords,
        None,
        false,
        overloads,
        () => impl
      )

    val add = addDyadVect(
      "+",
      "Addition",
      List("add", "+", "plus"),
      "a: num, b: num -> a + b",
      "a: num, b: str -> a + b",
      "a: str, b: num -> a + b",
      "a: str, b: str -> a + b"
    ) {
      case (a: VNum, b: VNum)     => a + b
      case (a: String, b: VNum)   => s"$a$b"
      case (a: VNum, b: String)   => s"$a$b"
      case (a: String, b: String) => s"$a$b"
      // todo consider doing something like APL's forks
    }

    val concatenate = addDyad(
      "&",
      "Concatenate",
      List("concat", "&&", "append"),
      "a: any, b: any -> a ++ b"
    ) {
      case (a: VList, b: VList) => VList(a ++ b*)
      case (a: VList, b: VAny)  => VList(a :+ b*)
      case (a: VAny, b: VList)  => VList(a +: b*)
      case (a: VNum, b: VNum)   => VNum.from(f"$a$b")
      case (a: VAny, b: VAny)   => add(a, b)
    }

    val divide = addDyadVect(
      "÷",
      "Divide | Split",
      List("divide", "div", "split"),
      "a: num, b: num -> a / b",
      "a: str, b: str -> Split a on the regex b"
    ) {
      case (a: VNum, b: VNum)     => a / b
      case (a: String, b: String) => VList(a.split(b)*)
    }

    val dup = addDirect(":", "Duplicate", List("dup"), "a -> a, a") { ctx ?=>
      val a = ctx.pop()
      ctx.push(a)
      ctx.push(a)
    }

    val equals = addDyadVect(
      "=",
      "Equals",
      List("eq", "==", "equal", "same?", "equals?", "equal?"),
      "a: any, b: any -> a == b"
    ) {
      case (a: VNum, b: VNum)     => a == b
      case (a: VNum, b: String)   => a.toString == b
      case (a: String, b: VNum)   => a == b.toString
      case (a: String, b: String) => a == b
    }

    val exponentation = addDyadVect(
      "*",
      "Exponentation | Remove Nth Letter | Trim",
      List("exp", "**", "pow", "exponent", "remove-letter", "str-trim"),
      "a: num, b: num -> a ^ b",
      "a: str, b: num -> a with the bth letter removed",
      "a: num, b: str -> b with the ath letter removed",
      "a: str, b: str -> trim b from both sides of a"
    ) {
      case (a: VNum, b: VNum)   => a.pow(b)
      case (a: String, b: VNum) => StringHelpers.remove(a, b.toInt)
      case (a: VNum, b: String) => StringHelpers.remove(b, a.toInt)
      case (a: String, b: String) =>
        a.dropWhile(_.toString == b)
          .reverse
          .dropWhile(_.toString == b)
          .reverse // https://stackoverflow.com/a/17995686/9363594
    }

    val factorial = addMonadVect(
      "!",
      "Factorial | To Uppercase",
      List("fact", "factorial", "to-upper", "upper", "uppercase", "!"),
      "a: num -> a!",
      "a: str -> a.toUpperCase()"
    ) {
      case a: VNum   => spire.math.fact(a.toLong)
      case a: String => a.toUpperCase()
    }

    val getContextVariable = addNilad(
      "n",
      "Get Context Variable",
      List("get-context", "context", "c-var", "ctx"),
      " -> context variable n"
    ) { ctx ?=> ctx.contextVar }

    val greaterThan = addDyadVect(
      ">",
      "Greater Than",
      List("gt", "greater", "greater-than", ">", "greater?", "bigger?"),
      "a: num, b: num -> a > b",
      "a: str, b: num -> a > str(b)",
      "a: num, b: str -> str(a) > b",
      "a: str, b: str -> a > b"
    ) {
      case (a: VNum, b: VNum)     => a > b
      case (a: String, b: VNum)   => a > b.toString
      case (a: VNum, b: String)   => a.toString > b
      case (a: String, b: String) => a > b
    }

    val lessThan: Dyad = addDyadVect(
      "<",
      "Less Than",
      List("lt", "less", "less-than", "<", "less?", "smaller?"),
      "a: num, b: num -> a < b",
      "a: str, b: num -> a < str(b)",
      "a: num, b: str -> str(a) < b",
      "a: str, b: str -> a < b"
    ) {
      case (a: VNum, b: VNum)     => a < b
      case (a: String, b: VNum)   => a < b.toString
      case (a: VNum, b: String)   => a.toString < b
      case (a: String, b: String) => a < b
    }

    val modulo: Dyad = addDyadVect(
      "%",
      "Modulo | String Formatting",
      List("mod", "modulo", "str-format", "format", "%"),
      "a: num, b: num -> a % b",
      "a: str, b: any -> a.format(b) (replace %s with b if scalar value or each item in b if vector)"
    ) {
      case (a: VNum, b: VNum)   => a.tmod(b)
      case (a: String, b: VAny) => StringHelpers.formatString(a, b)
      case (a: VAny, b: String) => StringHelpers.formatString(b, a)
    }

    val multiply = addDyadVect(
      "×",
      "Multiplication",
      List("mul", "multiply", "times", "str-repeat", "*", "ring-trans"),
      "a: num, b: num -> a * b",
      "a: num, b: str -> b repeated a times",
      "a: str, b: num -> a repeated b times",
      "a: str, b: str -> ring translate a according to b"
    ) {
      case (a: VNum, b: VNum)     => a * b
      case (a: String, b: VNum)   => a * b.toInt
      case (a: VNum, b: String)   => b * a.toInt
      case (a: String, b: String) => StringHelpers.ringTranslate(a, b)
      case (a: VFun, b: VNum)     => a.withArity(b.toInt)
    }

    val ordChr =
      addMonadVect(
        "O",
        "Ord/Chr",
        List("ord", "chr"),
        "a: str -> ord(a)",
        "a: num -> chr(a)"
      ) {
        case a: String =>
          if (a.length == 1) a.codePointAt(0)
          else VList(a.map(_.toInt: VNum)*)
        case a: VNum => a.toInt.toChar.toString
      }

    val pair = addDyad(";", "Pair", List("pair"), "a, b -> [a, b]") { (a, b) =>
      VList(a, b)
    }

    val print = addDirect(
      ",",
      "Print",
      List("print", "puts", "out"),
      "a -> printed to stdout"
    ) { ctx ?=>
      MiscHelpers.vyPrintln(ctx.pop())
    }

    val subtraction = addDyadVect(
      "-",
      "Subtraction",
      List(
        "sub",
        "subtract",
        "minus",
        "str-remove",
        "remove",
        "str-remove-all",
        "remove-all",
        "-"
      ),
      "a: num, b: num -> a - b",
      "a: str, b: num -> a + b '-'s",
      "a: num, b: str -> a '-'s + b",
      "a: str, b: str -> a with b removed"
    ) {
      case (a: VNum, b: VNum) => a - b
      case (a: String, b: VNum) =>
        a + "-" * b.toInt
      case (a: VNum, b: String) => "-" * a.toInt + b
      case (a: String, b: String) =>
        a.replace(b, "")
      // todo consider doing something like APL's forks
    }

    val swap = addDirect("$", "Swap", List("swap"), "a, b -> b, a") { ctx ?=>
      val b = ctx.pop()
      val a = ctx.pop()
      ctx.push(b)
      ctx.push(a)
    }

    // Constants

    addNilad("₀", "Ten", List("ten"), "10") { 10 }
    addNilad("₁", "Sixteen", List("sixteen"), "16") { 26 }
    addNilad("₂", "Twenty-six", List("twenty-six"), "26") { 26 }
    addNilad("₃", "Thirty-two", List("thirty-two"), "32") { 32 }
    addNilad("₄", "Sixty-four", List("sixty-four"), "64") { 64 }
    addNilad("₅", "One hundred", List("one-hundred"), "100") { 100 }
    addNilad(
      "₆",
      "One hundred twenty-eight",
      List("one-hundred-twenty-eight"),
      "128"
    ) { 128 }
    addNilad(
      "₇",
      "Two hundred fifty-six",
      List("two-hundred-fifty-six"),
      "256"
    ) { 256 }
    addNilad(
      "₈",
      "Alphabet",
      List("alphabet", "a-z"),
      "\"abcdefghijklmnopqrstuvwxyz\""
    ) {
      "abcdefghijklmnopqrstuvwxyz"
    }
    addNilad(
      "₉",
      "Empty array",
      List("empty-list", "nil-list", "new-list"),
      "[]"
    ) { VList.empty }

  }
}
