package pricemigrationengine.migrations.newspaper2024Migration
import java.time.LocalDate

object StringObfuscation {

  val string1: String = "0123456789AST-" // Do not edit this string!
  val string2: String = "456789AST0123&" // Do not edit this string!

  def obfuscate(x: Char): Char = {
    val index = string1.indexOf(x)
    if (index >= 0) {
      string2.charAt(index)
    } else {
      x
    }
  }

  def obfuscate(s: String): String = s.toList.map(obfuscate).mkString("")
}
