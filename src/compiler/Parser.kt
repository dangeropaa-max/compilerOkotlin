package compiler

import vm.OVM

class Parser(
    private val scanner: Scanner,
    private val errorHandler: ErrorHandler
) {
    private var hasError = false

    companion object {
        const val SP_ABS = 1
        const val SP_MAX = 2
        const val SP_MIN = 3
        const val SP_DEC = 4
        const val SP_ODD = 5
        const val SP_HALT = 6
        const val SP_INC = 7
        const val SP_INOPEN = 8
        const val SP_ININT = 9
        const val SP_OUTINT = 10
        const val SP_OUTLN = 11
    }

    private val table = TableOfName()
    private val ovm = OVM()
    private val codeGen = CodeGenerator(ovm)

    init {
        table.setErrorHandler(errorHandler)
    }

    fun compile() {
        table.openScope()
        table.addStandardItems()
        table.openScope()

        module()
        if (errorHandler.hasErrors) {
            println("\nОшибка компиляции\n")
            return
        }

        table.closeScope()
        table.closeScope()

        codeGen.printCode()
        codeGen.runCode()
    }
    private fun module() {
        checkLex(Lex.MODULE)
        errorIfNotExpectedLex(Lex.NAME)
        val moduleName = scanner.nameValue
        table.newItem(table.moduleItem(moduleName))
        scanner.nextLex()
        checkLex(Lex.SEMI)

        if (scanner.lex == Lex.IMPORT) {
            importPrc()
        }

        declarations()

        if (scanner.lex == Lex.BEGIN) {
            scanner.nextLex()
            statementSequence()
        }

        checkLex(Lex.END)
        errorIfNotExpectedLex(Lex.NAME)
        val item = table.findItem(scanner.nameValue)
        if (item?.typeOfItem != "module") {
            errorHandler.syntaxError("имя модуля")
        } else if (item.name != moduleName) {
            errorHandler.syntaxError("имя модуля ${scanner.nameValue}")
        }
        scanner.nextLex()
        checkLex(Lex.DOT)
        codeGen.genStop()
        locateVariables()
    }

    private fun locateVariables() {
        val vars = table.getVars()
        codeGen.gen(codeGen.cmdCounter)
        for (item in vars.reversed()) {
            if (item.addr.toInt() > 0) {
                codeGen.fillGaps(item.addr.toInt())
                codeGen.gen(0)
            } else {
                println("Переменная `${item.name}` объявлена, но не используется")
            }
        }
    }

    // src/main/kotlin/compiler/Parser.kt
// Замените эти методы:

    private fun importPrc() {
        checkLex(Lex.IMPORT)
        contextImport()
        while (scanner.lex == Lex.COMMA) {
            checkLex(Lex.COMMA)
            contextImport()
        }
        checkLex(Lex.SEMI)
    }

    private fun contextImport() {
        errorIfNotExpectedLex(Lex.NAME)
        val name = scanner.nameValue
        if (name == "In" || name == "Out") {
            // Добавляем модуль
            table.newItem(table.moduleItem(name))
            // Добавляем процедуры модуля
            when (name) {
                "In" -> {
                    table.newItem(table.procedureItem("In.Open"))
                    table.newItem(table.procedureItem("In.Int"))
                }
                "Out" -> {
                    table.newItem(table.procedureItem("Out.Int"))
                    table.newItem(table.procedureItem("Out.Ln"))
                }
            }
        } else {
            errorHandler.contextError("Ожидается модуль In или Out")
        }
        scanner.nextLex()
    }

    private fun declarations() {
        while (scanner.lex == Lex.CONST || scanner.lex == Lex.VAR) {
            if (scanner.lex == Lex.CONST) {
                checkLex(Lex.CONST)
                while (scanner.lex == Lex.NAME) {
                    constantDeclaration()
                    checkLex(Lex.SEMI)
                }
            } else {
                checkLex(Lex.VAR)
                while (scanner.lex == Lex.NAME) {
                    varDeclaration()
                    checkLex(Lex.SEMI)
                }
            }
        }
    }

    private fun constantDeclaration() {
        errorIfNotExpectedLex(Lex.NAME)
        val constName = scanner.nameValue
        scanner.nextLex()
        checkLex(Lex.EQ)
        val constValue = constExpression()
        table.newItem(table.constItem(constName, Item.ItemTypes.Integer, constValue.toString()))
    }

    private fun constExpression(): Int {
        var sign = 1
        if (scanner.lex == Lex.MINUS) {
            checkLex(Lex.MINUS)
            sign = -1
        } else if (scanner.lex == Lex.PLUS) {
            checkLex(Lex.PLUS)
        }

        val value = if (scanner.lex == Lex.NAME) {
            val item = table.findItem(scanner.nameValue)
            checkLex(Lex.NAME)
            if (item?.typeOfItem != "const") {
                errorHandler.contextError("имя константы")
                0
            } else {
                item.value.toInt()
            }
        } else if (scanner.lex == Lex.NUM) {
            val num = scanner.numValue
            checkLex(Lex.NUM)
            num
        } else {
            errorHandler.syntaxError("имя константы или число")
            0
        }
        return value * sign
    }

    private fun varDeclaration() {
        contextVar()
        while (scanner.lex == Lex.COMMA) {
            checkLex(Lex.COMMA)
            contextVar()
        }
        checkLex(Lex.COLON)
        typePrc()
    }

    private fun contextVar() {
        errorIfNotExpectedLex(Lex.NAME)
        table.newItem(table.varItem(scanner.nameValue, Item.ItemTypes.Integer, "0"))
        scanner.nextLex()
    }

    private fun typePrc() {
        errorIfNotExpectedLex(Lex.NAME)
        val item = table.findItem(scanner.nameValue)
        if (item?.typeOfItem != "type") {
            errorHandler.contextError("Необъявленное имя типа")
        }
        scanner.nextLex()
    }

    private fun statementSequence() {
        statement()
        while (scanner.lex == Lex.SEMI) {
            scanner.nextLex()
            statement()
        }
    }

    private fun statement() {
        when (scanner.lex) {
            Lex.NAME -> variableOrCall()
            Lex.IF -> ifStatement()
            Lex.WHILE -> whileStatement()
            else -> {}
        }
    }

    private fun variableOrCall() {
        errorIfNotExpectedLex(Lex.NAME)
        var item = table.findItem(scanner.nameValue)
        scanner.nextLex()

        if (item?.typeOfItem == "var") {
            codeGen.genAddress(item)
            checkLex(Lex.ASS)
            val exprType = expression()
            if (item.type != exprType) {
                errorHandler.contextError("Неверный тип при присваивании")
            }
            codeGen.genSave()
        } else if (item?.typeOfItem == "procedure" || item?.typeOfItem == "module") {

            if (scanner.lex == Lex.DOT) {
                // обработка модуль.процедура
                if (item.typeOfItem != "module") {
                    errorHandler.contextError("Ожидается имя модуля")
                }
                scanner.nextLex()
                errorIfNotExpectedLex(Lex.NAME)
                val procName = item.name + "." + scanner.nameValue
                item = table.findItem(procName)
                if (item?.typeOfItem != "procedure") {
                    errorHandler.contextError("Ожидается процедура")
                }
                scanner.nextLex()

                // Теперь обрабатываем параметры
                if (scanner.lex == Lex.LPAR) {
                    scanner.nextLex()
                    checkProcParameters(item!!)
                    checkLex(Lex.RPAR)
                } else if (item?.name == "Out.Ln" || item?.name == "In.Open") {
                    // процедуры без параметров → скобки не нужны
                    // ничего не делаем
                } else {
                    errorHandler.contextError("Ожидается '(' для параметров")
                }

            } else if (item?.typeOfItem == "module") {

                errorHandler.contextError("Ожидается '.' после имени модуля ${item.name}")

            } else if (item?.typeOfItem == "procedure") {

                if (scanner.lex == Lex.LPAR) {
                    scanner.nextLex()
                    checkProcParameters(item!!)
                    checkLex(Lex.RPAR)
                } else if (item?.name != "Out.Ln" && item?.name != "In.Open") {
                    errorHandler.contextError("Ожидается '('")
                }
            }

        } else {
            errorHandler.contextError("Ожидается имя переменной или процедуры")
        }
    }

    private fun checkProcParameters(item: Item) {
        when (item.name) {
            "HALT" -> {
                val value = constExpression()
                codeGen.genHalt(value)
            }
            "INC" -> {
                errorIsNotVariable()
                codeGen.genDup()
                codeGen.genLoad()
                if (scanner.lex == Lex.COMMA) {
                    scanner.nextLex()
                    val exprType = expression()
                    checkIntType(exprType)
                } else {
                    codeGen.gen(1)
                }
                codeGen.genAddition()
                codeGen.genSave()
            }
            "DEC" -> {
                errorIsNotVariable()
                codeGen.genDup()
                codeGen.genLoad()
                if (scanner.lex == Lex.COMMA) {
                    scanner.nextLex()
                    val exprType = expression()
                    checkIntType(exprType)
                } else {
                    codeGen.gen(1)
                }
                codeGen.genSubstraction()
                codeGen.genSave()
            }
            "In.Open" -> {}
            "In.Int" -> {
                errorIsNotVariable()
                codeGen.genInInt()
            }
            "Out.Int" -> {
                val exprType = expression()
                checkIntType(exprType)
                checkLex(Lex.COMMA)
                val widthType = expression()
                checkIntType(widthType)
                codeGen.genOutInt()
            }
            "Out.Ln" -> codeGen.genOutLn()
            else -> errorHandler.contextError("Неизвестная процедура")
        }
    }

    private fun errorIsNotVariable() {
        errorIfNotExpectedLex(Lex.NAME)
        val item = table.findItem(scanner.nameValue)
        if (item?.typeOfItem != "var") {
            errorHandler.contextError("Ожидается имя переменной")
        }
        codeGen.genAddress(item!!)
        scanner.nextLex()
    }

    private fun ifStatement() {
        checkLex(Lex.IF)
        val exprType = expression()
        checkBoolType(exprType)
        checkLex(Lex.THEN)

        val afterCondPos = codeGen.cmdCounter
        var lastGoTo = 0

        statementSequence()

        while (scanner.lex == Lex.ELSIF) {
            codeGen.genGoTo(lastGoTo)
            lastGoTo = codeGen.cmdCounter
            codeGen.fillGaps(afterCondPos)

            checkLex(Lex.ELSIF)
            val elsifType = expression()
            checkBoolType(elsifType)
            checkLex(Lex.THEN)

            statementSequence()
        }

        if (scanner.lex == Lex.ELSE) {
            codeGen.genGoTo(lastGoTo)
            lastGoTo = codeGen.cmdCounter
            codeGen.fillGaps(afterCondPos)

            checkLex(Lex.ELSE)
            statementSequence()
        } else {
            codeGen.fillGaps(afterCondPos)
        }

        checkLex(Lex.END)
        codeGen.fillGaps(lastGoTo)
    }

    private fun whileStatement() {
        val whilePos = codeGen.cmdCounter

        checkLex(Lex.WHILE)
        val exprType = expression()
        checkBoolType(exprType)

        val afterCondPos = codeGen.cmdCounter

        checkLex(Lex.DO)
        statementSequence()
        checkLex(Lex.END)

        codeGen.genGoTo(whilePos)
        codeGen.fillGaps(afterCondPos)
    }

    private fun expression(): Item.ItemTypes {
        var leftType = simpleExpression()

        when (scanner.lex) {
            Lex.EQ, Lex.NE, Lex.LT, Lex.LE, Lex.GT, Lex.GE -> {
                val op = scanner.lex
                checkIntType(leftType)
                scanner.nextLex()
                val rightType = simpleExpression()
                checkIntType(rightType)
                codeGen.genComparison(scanner.getLexString(op))
                return Item.ItemTypes.Boolean
            }
            else -> return leftType
        }
    }

    private fun simpleExpression(): Item.ItemTypes {
        var termType: Item.ItemTypes

        if (scanner.lex == Lex.PLUS || scanner.lex == Lex.MINUS) {
            val op = scanner.lex
            scanner.nextLex()
            termType = term()
            checkIntType(termType)
            if (op == Lex.MINUS) {
                codeGen.genNegative()
            }
        } else {
            termType = term()
        }

        while (scanner.lex == Lex.PLUS || scanner.lex == Lex.MINUS) {
            val op = scanner.lex
            scanner.nextLex()
            val rightType = term()
            checkIntType(termType)
            checkIntType(rightType)
            if (op == Lex.PLUS) {
                codeGen.genAddition()
            } else {
                codeGen.genSubstraction()
            }
        }
        return termType
    }

    private fun term(): Item.ItemTypes {
        var type = factor()
        //println("DEBUG term: после factor(), type=$type, lex=${scanner.lex}")

        while (scanner.lex == Lex.MULT || scanner.lex == Lex.DIV || scanner.lex == Lex.MOD) {
            val op = scanner.lex
            //println("DEBUG term: операция ${scanner.getLexString(op)}")
            scanner.nextLex()
            val rightType = factor()
            //println("DEBUG term: правый тип=$rightType")
            checkIntType(type)
            checkIntType(rightType)

            // Временно замените на явный вызов
            when (op) {
                Lex.MULT -> {
                    //println("DEBUG: ГЕНЕРАЦИЯ MULT")
                    codeGen.gen(OVM.MULT)
                }
                Lex.DIV -> {
                   // println("DEBUG: ГЕНЕРАЦИЯ DIV")
                    codeGen.gen(OVM.DIV)
                }
                Lex.MOD -> {
                    //println("DEBUG: ГЕНЕРАЦИЯ MOD")
                    codeGen.gen(OVM.MOD)
                }
                else -> {}
            }
        }
        return type
    }

    private fun factor(): Item.ItemTypes {
        return when (scanner.lex) {
            Lex.NUM -> {
                codeGen.genConst(scanner.numValue)
                scanner.nextLex()
                Item.ItemTypes.Integer
            }
            Lex.NAME -> {
                val item = table.findItem(scanner.nameValue)
                when (item?.typeOfItem) {
                    "const" -> {
                        codeGen.genConst(item.value.toInt())
                        scanner.nextLex()
                        item.type
                    }
                    "var" -> {
                        codeGen.genVar(item)
                        scanner.nextLex()
                        item.type
                    }
                    "function" -> {
                        scanner.nextLex()
                        checkLex(Lex.LPAR)
                        checkFuncParameters(item)
                        checkLex(Lex.RPAR)
                        item.type
                    }
                    else -> {
                        errorHandler.contextError("Ожидается константа, имя или функция")
                        Item.ItemTypes.Integer
                    }
                }
            }
            Lex.LPAR -> {
                scanner.nextLex()
                val type = expression()
                checkLex(Lex.RPAR)
                type
            }
            else -> {
                errorHandler.syntaxError("Имя, число или (")
                Item.ItemTypes.Integer
            }
        }
    }

    private fun checkFuncParameters(item: Item) {
        when (item.name) {
            "ABS" -> {
                val exprType = expression()
                checkIntType(exprType)
                codeGen.genFunc("ABS")
            }
            "MIN" -> {
                typePrc()
                codeGen.genFunc("MIN")
            }
            "MAX" -> {
                typePrc()
                codeGen.genFunc("MAX")
            }
            "ODD" -> {
                val exprType = expression()
                checkIntType(exprType)
                codeGen.genFunc("ODD")
            }
            else -> errorHandler.contextError("Неизвестная функция")
        }
    }

    private fun checkLex(lex: Lex) {
        if (hasError) return
        if (scanner.lex == lex) {
            scanner.nextLex()
        } else {
            errorHandler.syntaxError(scanner.getLexString(lex))
            hasError = true
        }
    }

    private fun errorIfNotExpectedLex(lex: Lex) {
        if (hasError) return
        if (scanner.lex != lex) {
            errorHandler.syntaxError(scanner.getLexString(lex))
            hasError = true
        }
    }

    private fun checkIntType(type: Item.ItemTypes) {
        if (type != Item.ItemTypes.Integer) {
            errorHandler.contextError("Ожидается целый тип")
        }
    }

    private fun checkBoolType(type: Item.ItemTypes) {
        if (type != Item.ItemTypes.Boolean) {
            errorHandler.contextError("Ожидается логический тип")
        }
    }
}