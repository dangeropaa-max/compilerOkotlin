package vm

class OVM {
    companion object {
        const val MEMORY_SIZE = 8192

        const val STOP = -1
        const val ADD = -2
        const val SUB = -3
        const val MULT = -4
        const val DIV = -5
        const val MOD = -6
        const val NEG = -7
        const val LOAD = -8
        const val SAVE = -9
        const val DUP = -10
        const val DROP = -11
        const val SWAP = -12
        const val OVER = -13
        const val GOTO = -14
        const val IFLT = -15
        const val IFLE = -16
        const val IFGT = -17
        const val IFGE = -18
        const val IFEQ = -19
        const val IFNE = -20
        const val IN = -21
        const val OUT = -22
        const val LN = -23
    }

    private val memory = IntArray(MEMORY_SIZE) { -1 }
    private var pc = 0
    private var sp = MEMORY_SIZE
    private var tickCounter = 0

    fun getMemory(): IntArray = memory

    fun run() {
        while (true) {
            val cmd = memory[pc]
            pc++
            tickCounter++

            if (cmd >= 0) {
                sp--
                memory[sp] = cmd
            } else {
                when (cmd) {
                    STOP -> break
                    ADD -> {
                        sp++
                        memory[sp] = memory[sp] + memory[sp - 1]
                    }
                    SUB -> {
                        sp++
                        memory[sp] = memory[sp] - memory[sp - 1]
                    }
                    MULT -> {
                        sp++
                        memory[sp] = memory[sp] * memory[sp - 1]
                    }
                    DIV -> {
                        sp++
                        memory[sp] = memory[sp] / memory[sp - 1]
                    }
                    MOD -> {
                        sp++
                        memory[sp] = memory[sp] % memory[sp - 1]
                    }
                    NEG -> {
                        memory[sp] = -memory[sp]
                    }
                    LOAD -> {
                        memory[sp] = memory[memory[sp]]
                    }
                    SAVE -> {
                        memory[memory[sp + 1]] = memory[sp]
                        sp += 2
                    }
                    DUP -> {
                        memory[sp - 1] = memory[sp]
                        sp--
                    }
                    DROP -> {
                        sp++
                    }
                    SWAP -> {
                        val tmp = memory[sp]
                        memory[sp] = memory[sp + 1]
                        memory[sp + 1] = tmp
                    }
                    OVER -> {
                        sp--
                        memory[sp] = memory[sp + 2]
                    }
                    GOTO -> {
                        pc = memory[sp]
                        sp++
                    }
                    IFEQ -> {
                        if (memory[sp + 2] == memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IFNE -> {
                        if (memory[sp + 2] != memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IFLT -> {
                        if (memory[sp + 2] < memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IFLE -> {
                        if (memory[sp + 2] <= memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IFGT -> {
                        if (memory[sp + 2] > memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IFGE -> {
                        if (memory[sp + 2] >= memory[sp + 1]) {
                            pc = memory[sp]
                        }
                        sp += 3
                    }
                    IN -> {
                        sp--
                        print("? ")
                        try {
                            memory[sp] = readln().toInt()
                        } catch (e: Exception) {
                            println("Неправильный ввод")
                            memory[sp] = 0
                        }
                    }
                    OUT -> {
                        val width = memory[sp]
                        val value = memory[sp + 1]
                        if (width <= 0) {
                            print(value)
                        } else {
                            print("%${width}d".format(value))
                        }
                        sp += 2
                    }
                    LN -> {
                        println()
                    }
                    else -> {
                        println("Недопустимая команда: $cmd")
                        break
                    }
                }
            }
        }

        println("\nКоличество тактов: $tickCounter")
        if (sp < MEMORY_SIZE) {
            println("Код возврата: ${memory[sp]}")
        }
    }

    fun printCode(limit: Int) {
        println()
        for (i in 0 until limit) {
            print("$i) ")
            if (memory[i] >= 0) {
                println(memory[i])
            } else {
                println(operationToString(memory[i]))
            }
        }
    }

    private fun operationToString(op: Int): String {
        return when (op) {
            STOP -> "STOP"
            ADD -> "ADD"
            SUB -> "SUB"
            MULT -> "MULT"
            DIV -> "DIV"
            MOD -> "MOD"
            NEG -> "NEG"
            LOAD -> "LOAD"
            SAVE -> "SAVE"
            DUP -> "DUP"
            DROP -> "DROP"
            SWAP -> "SWAP"
            OVER -> "OVER"
            GOTO -> "GOTO"
            IFLT -> "IFLT"
            IFLE -> "IFLE"
            IFGT -> "IFGT"
            IFGE -> "IFGE"
            IFEQ -> "IFEQ"
            IFNE -> "IFNE"
            IN -> "IN"
            OUT -> "OUT"
            LN -> "LN"
            else -> "ERROR"
        }
    }
}