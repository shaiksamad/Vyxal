package vyxal

import vyxal.*

import org.scalatest.funspec.AnyFunSpec

class LiterateTests extends VyxalTests:
  def testLiteral(input: String, expected: String) =
    assertResult(expected)(LiterateLexer.litLex(input))

  describe("Literals") {
    it("should leave numbers as-is") {
      group {
        testLiteral("123", "123")
        testLiteral("6.", "6.")
        testLiteral("3.4i1.2", "3.4ı1.2")
        testLiteral("3.4i1.", "3.4ı1.")
        testLiteral("3.4i.2", "3.4ı.2")
        testLiteral("3.4i.", "3.4ı.")
        testLiteral(".i.", ".ı.")
        testLiteral("3.4i", "3.4ı")
        testLiteral(".4", ".4")
        testLiteral(".", ".")
        testLiteral("1_000_000", "1000000")
        testLiteral("1_0______0", "100")
      }
    }

    it("should leave strings as-is") {
      testLiteral(""""Hello, Vyxal!"""", """"Hello, Vyxal!"""")
      testLiteral(
        raw""""Vyxal is what \"you\" want!"""",
        raw""""Vyxal is what \"you\" want!""""
      )
    }
  }
  describe("Comments") {
    it("should ignore them") {
      testLiteral("1 2 3 ## This is a comment", "1 2 3")
      testLiteral("## Hello, World!", "")
    }
  }

  describe("Lambdas") {
    it("should transpile them correctly") {
      testLiteral("10 { context-n add } map", "10λn+}M")
      testLiteral("{{{}}{}}", "λλλ}}λ}}")
      testLiteral("{}{}", "λ}λ}")
    }

    it("should do arguments correctly") {
      testLiteral("lambda x, y -> $x $y add end", "λx,y|#$x#$y+}")
      testLiteral("lambda add -> $add", "λadd|#$add")
      testLiteral("lambda lambda add -> $add end end", "λλadd|#$add}}")
      testLiteral("lambda add lambda add ->", "λ+λadd|")
    }
  }

  describe("Lists") {
    it("should transpile them correctly") {
      testLiteral("[1|2|3|4]", "#[1|2|3|4#]")
      testLiteral("[]", "#[#]")
      testLiteral("[[]|[]]", "#[#[#]|#[#]#]")
    }
  }

  describe("Variable Get") {
    it("should transpile them correctly") {
      testLiteral("$a", "#$a")
      testLiteral("$", "#$")
    }
  }

  describe("Variable Set") {
    it("should transpile them correctly") {
      testLiteral("10 :=x", "10#=x")
      testLiteral(":=x", "#=x")
    }
  }

  describe("Constants") {
    it("should transpile them correctly") {
      testLiteral(":!=x", "#!x")
      testLiteral("10 :!=x", "10#!x")
    }
  }

  describe("Variable Augmentation") {
    it("should transpile them correctly") {
      testLiteral("10 +:>x", "10+#>x")
      testLiteral("+:>x", "+#>x")
    }
  }

  describe("Variable unpacking") {
    it("should transpile them correctly") {
      testLiteral("[1|2|3] :=[x|y|z]", "#[1|2|3#]#:[x|y|z]")
    }
  }

  describe("Ungrouping") {
    it("should remove the parentheses") {
      testLiteral("1 (3 4 add) times", "1 3 4+×")
      testLiteral("(((((add)))))", "+")
    }
  }

  describe("Misc") {
    it("should not treat words with i as complex") {
      testLiteral("is-vowel?", "A")
      testLiteral("is-vowel? i", "Aı")
      testLiteral("i is-vowel?", "ı A")
    }
  }
end LiterateTests