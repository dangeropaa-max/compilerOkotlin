import compiler.*
import vm.OVM
import java.io.File

fun main(args: Array<String>) {
    println("=== Компилятор языка «О» ===\n")

    if (args.isEmpty()) {
        println("Использование: java -jar out.jar <файл.o>")
        return
    }

    val fileName = args[0]
    val file = File(fileName)

    if (!file.exists()) {
        println("Файл не найден: $fileName")
        return
    }

    val source = file.readText()

    // Выводим исходный код
    println("Исходный код из файла: $fileName")
    println("=".repeat(50))
    println(source)
    println("=".repeat(50))
    println()

    val errorHandler = ErrorHandler()
    errorHandler.setSource(source)

    val scanner = Scanner(source, errorHandler)
    val parser = Parser(scanner, errorHandler)
    parser.compile()

    println("\nСкомпилировано")
}