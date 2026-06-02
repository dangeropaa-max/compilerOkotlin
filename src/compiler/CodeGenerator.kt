package compiler

import vm.OVM

class CodeGenerator(private val ovm: OVM) {
    var cmdCounter = 0
        private set

    fun gen(cmd: Int) {
        ovm.getMemory()[cmdCounter] = cmd
        cmdCounter++
    }

    fun genConst(constValue: Int) {
        if (constValue >= 0) {
            gen(constValue)
        } else {
            gen(-constValue)
            gen(OVM.NEG)
        }
    }

    fun genVar(item: Item) {
        genAddress(item)
        gen(OVM.LOAD)
    }

    fun genAddress(item: Item) {
        gen(item.addr.toInt())
        item.addr = (cmdCounter + 1).toString()
    }

    fun genFunc(func: String) {
        when (func) {
            "ABS" -> {
                gen(OVM.DUP)
                gen(0)
                gen(cmdCounter + 3)
                gen(OVM.IFGE)
                gen(OVM.NEG)
            }
            "MAX" -> {
                gen(Int.MAX_VALUE)
            }
            "MIN" -> {
                gen(Int.MAX_VALUE)
                gen(OVM.NEG)
                gen(1)
                gen(OVM.SUB)
            }
            "ODD" -> {
                gen(2)
                gen(OVM.MOD)
                gen(0)
                gen(0)
                gen(OVM.IFEQ)
            }
        }
    }

    fun genOperation(operation: String) {
        when (operation) {
            "DIV" -> gen(OVM.DIV)
            "MULT" -> gen(OVM.MULT)
            "MOD" -> gen(OVM.MOD)
        }
    }

    fun genNegative() = gen(OVM.NEG)
    fun genAddition() = gen(OVM.ADD)
    fun genSubstraction() = gen(OVM.SUB)
    fun genSave() = gen(OVM.SAVE)
    fun genDup() = gen(OVM.DUP)
    fun genLoad() = gen(OVM.LOAD)

    fun genInInt() {
        gen(OVM.IN)
        gen(OVM.SAVE)
    }

    fun genOutInt() = gen(OVM.OUT)
    fun genOutLn() = gen(OVM.LN)

    fun genHalt(exitCode: Int) {
        genConst(exitCode)
        gen(OVM.STOP)
    }

    fun genComparison(operation: String) {
        gen(0)
        when (operation) {
            "=" -> gen(OVM.IFNE)
            "#" -> gen(OVM.IFEQ)
            "<" -> gen(OVM.IFGE)
            "<=" -> gen(OVM.IFGT)
            ">" -> gen(OVM.IFLE)
            ">=" -> gen(OVM.IFLT)
        }
    }

    fun genGoTo(code: Int) {
        gen(code)
        gen(OVM.GOTO)
    }

    fun fillGaps(addrWithGaps: Int) {
        var current = addrWithGaps
        while (current > 0) {
            val tmp = ovm.getMemory()[current - 2]
            ovm.getMemory()[current - 2] = cmdCounter
            current = tmp
        }
    }

    fun genStop() {
        gen(OVM.STOP)
    }

    fun runCode() {
        ovm.run()
    }

    fun printCode() {
        ovm.printCode(cmdCounter)
    }
}