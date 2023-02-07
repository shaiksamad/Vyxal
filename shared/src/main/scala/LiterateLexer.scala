package vyxal

import java.util.regex.Pattern
import scala.util.matching.Regex
import scala.util.parsing.combinator.*
import LiterateToken.*

enum LiterateToken(val value: Object):
  case Word(override val value: String) extends LiterateToken(value)
  case AlreadyCode(override val value: String) extends LiterateToken(value)
  case LitComment(override val value: String) extends LiterateToken(value)
  case LambdaBlock(override val value: String) extends LiterateToken(value)

  case ListToken(override val value: List[Object])
      extends LiterateToken(value.toString)
object LiterateLexer extends RegexParsers:
  override def skipWhitespace = true
  override val whiteSpace: Regex = "[ \t\r\f]+".r

  private def decimalRegex = raw"((0|[1-9][0-9]*)?\.[0-9]*|0|[1-9][0-9]*)"
  def number: Parser[LiterateToken] =
    raw"($decimalRegex?ı$decimalRegex?)|$decimalRegex".r ^^ { value =>
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
    """\{""".r ~ rep(lambdaBlock | """[^{}]+""".r) ~ """\}""".r ^^ {
      case _ ~ body ~ _ => LambdaBlock(body.mkString)
    }
  def normalGroup: Parser[LiterateToken] =
    """\(""".r ~ rep(normalGroup | """[^()]+""".r) ~ """\)""".r ^^ {
      case _ ~ body ~ _ => Word(body.mkString)
    }

  def list: Parser[LiterateToken] =
    "[" ~ repsep(list | """[^\]\[|]+""".r, "|") ~ "]" ^^ { case _ ~ body ~ _ =>
      ListToken(body)
    }

  def word: Parser[LiterateToken] =
    """[a-zA-Z?!*+=&%><-][a-zA-Z0-9?!*+=&%><-]*""".r ^^ { value =>
      Word(value)
    }

  def varGet: Parser[LiterateToken] = """\$([a-zA-Z][a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#" + value)
  }

  def varSet: Parser[LiterateToken] = """=([a-zA-Z][a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#" + value)
  }

  def augVar: Parser[LiterateToken] = """:=([a-zA-Z][a-zA-Z0-9]*)?""".r ^^ {
    value => AlreadyCode("#>" + value.substring(2))
  }

  def unpackVar: Parser[LiterateToken] = "=" ~ list ^^ { case _ ~ value =>
    (value: @unchecked) match
      case ListToken(value) =>
        println(value.map(recHelp).mkString("[", "|", "]"))
        AlreadyCode("#:" + value.map(recHelp).mkString("[", "|", "]"))
  }

  def tokens: Parser[List[LiterateToken]] = phrase(
    rep(
      number | string | singleCharString | comment | list | lambdaBlock | normalGroup | unpackVar | varGet | varSet | augVar | word
    )
  )

  def apply(code: String): Either[VyxalCompilationError, String] =
    (parse(tokens, code): @unchecked) match
      case NoSuccess(msg, next)  => Left(VyxalCompilationError(msg))
      case Success(result, next) => Right(sbcsify(result))
end LiterateLexer

def recHelp(token: Object): String =
  token match
    case Word(value)        => value
    case AlreadyCode(value) => value
    case LitComment(value)  => value
    case LambdaBlock(value) => value
    case ListToken(value)   => value.map(recHelp).mkString("[", "|", "]")
    case value: String      => value

def sbcsify(tokens: List[LiterateToken]): String =
  tokens.map(sbcsify).mkString("", " ", "")

def sbcsify(token: LiterateToken): String =
  token match
    case Word(value)        => value
    case AlreadyCode(value) => value
    case LitComment(value)  => ""
    case LambdaBlock(value) => getRight(LiterateLexer(value))
    case ListToken(value)   => value.map(recHelp).mkString("[", "|", "]")

def getRight(either: Either[VyxalCompilationError, String]): String =
  either match
    case Right(value) => value
    case Left(value)  => value.toString
