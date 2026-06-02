package compiler

class Scanner(private val source: String, private val errorHandler: ErrorHandler) {
    companion object {
        const val NAMELEN = 64
    }

    var lex: Lex = Lex.NONE
    var nameValue: String = ""
    var numValue: Int = 0

    private var pos = 0
    private var line = 1
    private var column = 1
    private var ch: Char = if (source.isNotEmpty()) source[0] else '\u0000'



    private val keywords = mapOf(
        "MODULE" to Lex.MODULE,
        "IMPORT" to Lex.IMPORT,
        "CONST" to Lex.CONST,
        "VAR" to Lex.VAR,
        "BEGIN" to Lex.BEGIN,
        "END" to Lex.END,
        "IF" to Lex.IF,
        "THEN" to Lex.THEN,
        "ELSIF" to Lex.ELSIF,
        "ELSE" to Lex.ELSE,
        "WHILE" to Lex.WHILE,
        "DO" to Lex.DO,
        "DIV" to Lex.DIV,
        "MOD" to Lex.MOD,
        "ARRAY" to Lex.NONE,
        "RECORD" to Lex.NONE,
        "POINTER" to Lex.NONE,
        "SET" to Lex.NONE,
        "WITH" to Lex.NONE,
        "CASE" to Lex.NONE,
        "OF" to Lex.NONE,
        "LOOP" to Lex.NONE,
        "EXIT" to Lex.NONE,
        "PROCEDURE" to Lex.NONE,
        "FOR" to Lex.NONE,
        "TO" to Lex.NONE,
        "BY" to Lex.NONE,
        "IN" to Lex.NONE,
        "IS" to Lex.NONE,
        "NIL" to Lex.NONE,
        "OR" to Lex.NONE,
        "TYPE" to Lex.NONE,
        "REPEAT" to Lex.NONE,
        "UNTIL" to Lex.NONE,
        "RETURN" to Lex.NONE
    )

    init {
        errorHandler.setSource(source)
        nextLex()
    }

    private fun nextChar() {
        pos++
        column++

        if (pos < source.length) {
            ch = source[pos]

            // Обработка Windows (CRLF)
            if (ch == '\r') {
                // Пропускаем CR, смотрим следующий символ
                if (pos + 1 < source.length && source[pos + 1] == '\n') {
                    pos++
                    column = 1
                    line++
                    ch = '\n'
                } else {
                    // Просто CR без LF
                    line++
                    column = 1
                    ch = '\n'
                }
            }
            // Обработка Unix/Linux (LF)
            else if (ch == '\n') {
                line++
                column = 1
            }
            // Остальные символы — без изменений
        } else {
            ch = '\u0000'  // конец файла
        }
    }

    fun nextLex() {
        while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            if (ch == '\n') {
                line++
                column = 1
            }
            nextChar()
        }

        errorHandler.setPosition(line - 1, column - 1)

        when {
            ch.isLetter() -> scanName()
            ch.isDigit() -> scanNum()
            ch == ';' -> { lex = Lex.SEMI; nextChar() }
            ch == ':' -> {
                nextChar()
                if (ch == '=') {
                    lex = Lex.ASS
                    nextChar()
                } else {
                    lex = Lex.COLON
                }
            }
            ch == '.' -> { lex = Lex.DOT; nextChar() }
            ch == ',' -> { lex = Lex.COMMA; nextChar() }
            ch == '+' -> { lex = Lex.PLUS; nextChar() }
            ch == '-' -> { lex = Lex.MINUS; nextChar() }
            ch == '*' -> { lex = Lex.MULT; nextChar() }
            ch == '(' -> {
                nextChar()
                if (ch == '*') {
                    skipComment()
                    nextLex()
                } else {
                    lex = Lex.LPAR
                }
            }
            ch == ')' -> { lex = Lex.RPAR; nextChar() }
            ch == '#' -> { lex = Lex.NE; nextChar() }
            ch == '=' -> { lex = Lex.EQ; nextChar() }
            ch == '<' -> {
                nextChar()
                if (ch == '=') {
                    lex = Lex.LE
                    nextChar()
                } else {
                    lex = Lex.LT
                }
            }
            ch == '>' -> {
                nextChar()
                if (ch == '=') {
                    lex = Lex.GE
                    nextChar()
                } else {
                    lex = Lex.GT
                }
            }
            ch == '\u0000' -> lex = Lex.EOT
            else -> {
                errorHandler.lexError("Недопустимый символ: $ch")
                nextChar()
                nextLex()
            }
        }
    }

    private fun scanName() {
        nameValue = ""
        while (ch.isLetterOrDigit() && nameValue.length < NAMELEN) {
            nameValue += ch
            nextChar()
        }

        // Если имя длиннее NAMELEN
        if (ch.isLetterOrDigit()) {
            errorHandler.lexError("Имя слишком длинное (максимум $NAMELEN символов)")
            // Пропускаем остальные символы имени
            while (ch.isLetterOrDigit()) {
                nextChar()
            }
        }

        lex = keywords[nameValue] ?: Lex.NAME
    }

    private fun scanNum() {
        numValue = 0
        while (ch.isDigit()) {
            val digit = ch - '0'
            if (numValue <= (Int.MAX_VALUE - digit) / 10) {
                numValue = numValue * 10 + digit
            } else {
                errorHandler.lexError("Переполнение")
            }
            nextChar()
        }
        lex = Lex.NUM
    }

    private fun skipComment() {
        while (true) {
            while (ch != '*' && ch != '\u0000') {
                nextChar()
                if (ch == '(') {
                    nextChar()
                    if (ch == '*') {
                        skipComment()
                    }
                }
            }
            if (ch == '\u0000') {
                errorHandler.lexError("Нет конца комментария")
                return
            }
            nextChar()
            if (ch == ')') {
                nextChar()
                return
            }
        }
    }

    fun getLexString(lex: Lex): String {
        return when (lex) {
            Lex.NAME -> "имя"
            Lex.NUM -> "число"
            Lex.EQ -> "="
            Lex.NE -> "#"
            Lex.LT -> "<"
            Lex.LE -> "<="
            Lex.GT -> ">"
            Lex.GE -> ">="
            Lex.ASS -> ":="
            Lex.SEMI -> ";"
            Lex.COLON -> ":"
            Lex.DOT -> "."
            Lex.COMMA -> ","
            Lex.LPAR -> "("
            Lex.RPAR -> ")"
            Lex.PLUS -> "+"
            Lex.MINUS -> "-"
            Lex.MULT -> "*"
            Lex.DIV -> "DIV"
            Lex.MOD -> "MOD"
            Lex.IF -> "IF"
            Lex.THEN -> "THEN"
            Lex.ELSIF -> "ELSIF"
            Lex.ELSE -> "ELSE"
            Lex.WHILE -> "WHILE"
            Lex.DO -> "DO"
            Lex.BEGIN -> "BEGIN"
            Lex.END -> "END"
            Lex.CONST -> "CONST"
            Lex.VAR -> "VAR"
            Lex.MODULE -> "MODULE"
            Lex.IMPORT -> "IMPORT"
            else -> "лексема"
        }
    }

    fun expect(expected: Lex) {
        if (lex != expected) {
            errorHandler.syntaxError(getLexString(expected))
        }
        nextLex()
    }
}