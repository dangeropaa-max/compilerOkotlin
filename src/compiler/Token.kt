package compiler

data class Token(
    val type: Lex,
    val value: Any? = null,
    val line: Int = 0,
    val column: Int = 0
)