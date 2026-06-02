package compiler

data class Item(
    var name: String,
    var typeOfItem: String,  // "module", "const", "var", "type", "procedure", "function"
    var type: ItemTypes = ItemTypes.Integer,
    var value: String = "",
    var addr: String = "0"
) {
    enum class ItemTypes {
        Integer, Boolean
    }
}