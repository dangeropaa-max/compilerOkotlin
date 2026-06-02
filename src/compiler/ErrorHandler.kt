package compiler

class ErrorHandler {
    var hasErrors = false
        private set

    private var sourceLines: List<String> = emptyList()
    private var currentLine: Int = 0
    private var currentColumn: Int = 0
    private var lexPosition: Int = 0

    fun setSource(source: String) {
        sourceLines = source.lines()
    }

    fun setPosition(line: Int, column: Int) {
        currentLine = line
        currentColumn = column
    }

    fun setLexPosition(pos: Int) {
        lexPosition = pos
    }

    fun syntaxError(expected: String) {
        hasErrors = true
        printError(lexPosition, "СИНТАКСИЧЕСКАЯ ОШИБКА", "Ожидается: $expected")
    }

    fun contextError(msg: String) {
        hasErrors = true
        printError(lexPosition, "КОНТЕКСТНАЯ ОШИБКА", msg)
    }

    fun lexError(msg: String) {
        hasErrors = true
        printError(lexPosition, "ЛЕКСИЧЕСКАЯ ОШИБКА", msg)
    }

    fun error(msg: String) {
        hasErrors = true
        printError(lexPosition, "ОШИБКА", msg)
    }

    private fun printError(pos: Int, type: String, message: String) {
        println("\n$type в строке ${currentLine + 1}, позиция $pos")
        if (currentLine < sourceLines.size) {
            val line = sourceLines[currentLine]
            println(line)
            val arrow = " ".repeat(pos.coerceAtMost(line.length)) + "^"
            println(arrow)
        }
        println("$type: $message")
    }

    fun fatalError(msg: String): Nothing {
        println("КРИТИЧЕСКАЯ ОШИБКА: $msg")
        kotlin.system.exitProcess(1)
    }
}