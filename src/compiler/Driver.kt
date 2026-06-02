package compiler

import java.io.File

class Driver(private val errorHandler: ErrorHandler) {
    companion object {
        const val chSpace = ' '
        const val chTab = '\t'
        const val chEOL = '\r'
        const val chEOT = '\u0000'
    }

    var ch: Char = chEOT
    var line: Int = 0
    var position: Int = 0

    private var inputChain: List<Char> = emptyList()
    private var index = 0

    fun resetText(path: String) {
        val file = File(path)
        if (!file.exists()) {
            errorHandler.error("Не удалось открыть файл")
        }
        val content = file.readText()
        inputChain = content.toList()
        index = 0
        nextCh()
    }

    fun nextCh() {
        if (index < inputChain.size) {
            ch = inputChain[index]
            index++
            position++
            print(ch)
            if (ch == '\n' || ch == '\r') {
                ch = chEOL
                position = 0
            }
        } else {
            ch = chEOT
        }
    }
}