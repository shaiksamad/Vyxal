package vyxal

import java.util.regex.Pattern
import scala.util.matching.Regex
import scala.util.parsing.combinator.*
import LiterateToken.*

val hardcodedKeywords = Map(
  "if" -> "[",
  "endif" -> "}",
  "end-if" -> "}",
  "for" -> "(",
  "endfor" -> "}",
  "end-for" -> "}",
  "while" -> "{",
  "endwhile" -> "}",
  "end-while" -> "}",
  "lambda" -> "λ",
  "endlambda" -> "}",
  "end-lambda" -> "}",
  "end" -> "}",
  "else" -> "|",
  "do-to-each" -> "(",
  "body" -> "|",
  "branch" -> "|",
  "close-all" -> "]"
)

enum LiterateToken(val value: Object):
  case Word(override val value: String) extends LiterateToken(value)
  case AlreadyCode(override val value: String) extends LiterateToken(value)
  case LitComment(override val value: String) extends LiterateToken(value)
  case LambdaBlock(override val value: List[Object])
      extends LiterateToken(value.toString)

  case ListToken(override val value: List[Object])
      extends LiterateToken(value.toString)
object LiterateLexer extends RegexParsers:
  override def skipWhitespace = true
  override val whiteSpace: Regex = "[ \t\r\f]+".r

  private def decimalRegex = raw"((0|[1-9][0-9]*)?\.[0-9]*|0|[1-9][0-9]*)"
  def number: Parser[LiterateToken] =
    raw"($decimalRegex?[ij]$decimalRegex?)|$decimalRegex".r ^^ { value =>
      AlreadyCode(value)
    }

  def string: Parser[LiterateToken] = raw"""("(?:[^"\\]|\\.)*["])""".r ^^ {
    value => Word(value.replaceAll("\\\\\"", "\""))
  }

  def singleCharString: Parser[LiterateToken] = """'.""".r ^^ { value =>
    Word(value)
  }

  def comment: Parser[LiterateToken] = """##[^\n]*""".r ^^ { value =>
    LitComment(value)
  }

  def lambdaBlock: Parser[LiterateToken] =
    """\{""".r ~ rep(lambdaBlock | """(#}|[^{}])+""".r) ~ """\}""".r ^^ {
      case _ ~ body ~ _ => LambdaBlock(body)
    }
  def normalGroup: Parser[LiterateToken] =
    """\(""".r ~ rep(normalGroup | """[^()]+""".r) ~ """\)""".r ^^ {
      case _ ~ body ~ _ => Word(body.map(recHelp).mkString)
    }

  def list: Parser[LiterateToken] =
    "[" ~ repsep(list | """[^\]\[|]+""".r, "|") ~ "]" ^^ { case _ ~ body ~ _ =>
      ListToken(body)
    }

  def word: Parser[LiterateToken] =
    """[a-zA-Z?!*+=&%><-][a-zA-Z0-9?!*+=&%><-]*""".r ^^ { value =>
      Word(value)
    }

  def varGet: Parser[LiterateToken] = """\$([_a-zA-Z][_a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#" + value)
  }

  def varSet: Parser[LiterateToken] = """:=([_a-zA-Z][_a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#" + value.substring(1))
  }

  def augVar: Parser[LiterateToken] = """:>([a-zA-Z][_a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#>" + value.substring(2))
  }

  def unpackVar: Parser[LiterateToken] = ":=" ~ list ^^ { case _ ~ value =>
    (value: @unchecked) match
      case ListToken(value) =>
        println(value.map(recHelp).mkString("[", "|", "]"))
        AlreadyCode("#:" + value.map(recHelp).mkString("[", "|", "]"))
  }

  def branch: Parser[LiterateToken] = "|" ^^ { value =>
    AlreadyCode("|")
  }

  def rawCode: Parser[LiterateToken] = "#([^#]|#[^}])*#}".r ^^ { value =>
    AlreadyCode(value.substring(1, value.length - 2))
  }

  def tokens: Parser[List[LiterateToken]] = phrase(
    rep(
      number | string | singleCharString | comment | rawCode | list | lambdaBlock | normalGroup | unpackVar | varGet | varSet | augVar | word | branch
    )
  )

  def apply(code: String): Either[VyxalCompilationError, List[LiterateToken]] =
    (parse(tokens, code): @unchecked) match
      case NoSuccess(msg, next)  => Left(VyxalCompilationError(msg))
      case Success(result, next) => Right(result)
end LiterateLexer

def recHelp(token: Object): String =
  token match
    case Word(value)        => value
    case AlreadyCode(value) => value
    case LitComment(value)  => value
    case LambdaBlock(value) => value.map(recHelp).mkString("λ", " ", "}")
    case ListToken(value)   => value.map(recHelp).mkString("[", "|", "]")
    case value: String      => value

def sbcsify(tokens: List[LiterateToken]): String =
  tokens.map(sbcsify).mkString("", " ", "")

def sbcsify(token: Object): String =
  token match
    case Word(value) =>
      literateModeMappings.getOrElse(
        value,
        hardcodedKeywords.getOrElse(value, value)
      )
    case AlreadyCode(value) => value
    case LitComment(value)  => ""
    case LambdaBlock(value) => value.map(sbcsify).mkString("λ", " ", "}")
    case ListToken(value)   => value.map(sbcsify).mkString("#[", "|", "#]")
    case value: String      => litLex(value)

def getRight(
    either: Either[VyxalCompilationError, List[LiterateToken]]
): List[LiterateToken] =
  either match
    case Right(value) => value
    case Left(value) =>
      println(value)
      List()

def litLex(code: String): String =
  sbcsify(getRight(LiterateLexer(code)))
