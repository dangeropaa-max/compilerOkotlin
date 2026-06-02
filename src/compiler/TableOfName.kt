package compiler

class TableOfName {
    private val table = mutableListOf<MutableMap<String, Item>>()
    private var errorHandler: ErrorHandler? = null

    fun setErrorHandler(handler: ErrorHandler) {
        errorHandler = handler
    }

    fun openScope() {
        table.add(mutableMapOf())
    }

    fun closeScope() {
        if (table.size > 1) {
            table.removeAt(table.lastIndex)
        }
    }

    fun addItem(item: Item) {
        table.last()[item.name] = item
    }

    fun newItem(item: Item) {
        if (table.isEmpty()) {
            errorHandler?.contextError("Нет открытой области видимости")
            return
        }
        val last = table.last()
        if (last.containsKey(item.name)) {
            errorHandler?.contextError("Повторное объявление имени: ${item.name}")
        } else {
            addItem(item)
        }
    }

    fun findItem(name: String): Item? {
        for (i in table.lastIndex downTo 0) {
            val item = table[i][name]
            if (item != null) return item
        }
        errorHandler?.contextError("Необъявленное имя: $name")
        return null
    }

    fun getVars(): List<Item> {
        return table.last().values.filter { it.typeOfItem == "var" }
    }

    fun moduleItem(name: String): Item {
        return Item(name, "module")
    }

    fun constItem(name: String, type: Item.ItemTypes, value: String): Item {
        return Item(name, "const", type, value)
    }

    fun varItem(name: String, type: Item.ItemTypes, addr: String): Item {
        return Item(name, "var", type, addr = addr)
    }

    fun typeItem(name: String, type: Item.ItemTypes): Item {
        return Item(name, "type", type)
    }

    fun functionItem(name: String, type: Item.ItemTypes): Item {
        return Item(name, "function", type)
    }

    fun procedureItem(name: String): Item {
        return Item(name, "procedure")
    }

    fun addStandardItems() {
        // Типы
        newItem(typeItem("INTEGER", Item.ItemTypes.Integer))



        // Функции
        newItem(functionItem("ABS", Item.ItemTypes.Integer))
        newItem(functionItem("ODD", Item.ItemTypes.Boolean))
        newItem(functionItem("MAX", Item.ItemTypes.Integer))
        newItem(functionItem("MIN", Item.ItemTypes.Integer))

        // Процедуры
        newItem(procedureItem("HALT"))
        newItem(procedureItem("INC"))
        newItem(procedureItem("DEC"))
        newItem(procedureItem("In.Open"))
        newItem(procedureItem("In.Int"))
        newItem(procedureItem("Out.Int"))
        newItem(procedureItem("Out.Ln"))


    }
}