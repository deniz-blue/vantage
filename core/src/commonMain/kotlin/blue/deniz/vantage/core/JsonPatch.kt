package blue.deniz.vantage.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonPatchException(message: String, val isFailedTest: Boolean = false) : Exception(message)

fun JsonElement.patched(operations: JsonArray): JsonElement {
    var document = this
    for (operation in operations) document = document.patchedBy(operation as? JsonObject ?: throw JsonPatchException("A patch operation is not an object"))
    return document
}

fun jsonPointerOf(vararg tokens: String): String =
    tokens.joinToString("") { "/" + it.replace("~", "~0").replace("/", "~1") }

private fun JsonElement.patchedBy(operation: JsonObject): JsonElement {
    val path = pointerTokens(operation.string("path"))
    return when (val name = operation.string("op")) {
        "add" -> added(path, operation.value())
        "remove" -> removed(path)
        "replace" -> removed(path).added(path, operation.value())
        "move" -> {
            val from = pointerTokens(operation.string("from"))
            removed(from).added(path, valueAt(from))
        }
        "copy" -> added(path, valueAt(pointerTokens(operation.string("from"))))
        "test" -> also {
            if (valueAt(path) != operation.value()) throw JsonPatchException("${operation.string("path")} is not the tested value", isFailedTest = true)
        }
        else -> throw JsonPatchException("Unknown patch operation $name")
    }
}

private fun JsonObject.string(key: String): String =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw JsonPatchException("A patch operation has no $key")

private fun JsonObject.value(): JsonElement = get("value") ?: throw JsonPatchException("A patch operation has no value")

private fun pointerTokens(pointer: String): List<String> {
    if (pointer.isEmpty()) return emptyList()
    if (!pointer.startsWith("/")) throw JsonPatchException("$pointer is not a JSON pointer")
    return pointer.substring(1).split("/").map { it.replace("~1", "/").replace("~0", "~") }
}

private fun JsonElement.valueAt(path: List<String>): JsonElement {
    var node = this
    for (token in path) node = node.child(token) ?: throw JsonPatchException("Nothing at ${jsonPointerOf(*path.toTypedArray())}")
    return node
}

private fun JsonElement.child(token: String): JsonElement? = when (this) {
    is JsonObject -> get(token)
    is JsonArray -> token.toIntOrNull()?.let { getOrNull(it) }
    else -> null
}

private fun JsonElement.added(path: List<String>, value: JsonElement): JsonElement =
    if (path.isEmpty()) value else edited(path) { parent, token ->
        when (parent) {
            is JsonObject -> JsonObject(parent + (token to value))
            is JsonArray -> {
                val index = if (token == "-") parent.size else arrayIndex(token, parent.size + 1)
                JsonArray(parent.toMutableList().apply { add(index, value) })
            }
            else -> throw JsonPatchException("Can't add to a value that is not an object or array")
        }
    }

private fun JsonElement.removed(path: List<String>): JsonElement {
    if (path.isEmpty()) throw JsonPatchException("Can't remove the whole document")
    return edited(path) { parent, token ->
        when (parent) {
            is JsonObject -> if (token in parent) JsonObject(parent - token) else throw JsonPatchException("Nothing to remove at $token")
            is JsonArray -> JsonArray(parent.toMutableList().apply { removeAt(arrayIndex(token, parent.size)) })
            else -> throw JsonPatchException("Can't remove from a value that is not an object or array")
        }
    }
}

private fun arrayIndex(token: String, size: Int): Int =
    token.takeIf { it == "0" || !it.startsWith("0") }?.toIntOrNull()?.takeIf { it in 0 until size }
        ?: throw JsonPatchException("$token is not an index in an array of $size")

private fun JsonElement.edited(path: List<String>, change: (parent: JsonElement, token: String) -> JsonElement): JsonElement {
    if (path.size == 1) return change(this, path[0])
    val token = path[0]
    val child = child(token) ?: throw JsonPatchException("Nothing at $token")
    val changed = child.edited(path.drop(1), change)
    return when (this) {
        is JsonObject -> JsonObject(this + (token to changed))
        is JsonArray -> JsonArray(toMutableList().apply { set(arrayIndex(token, size), changed) })
        else -> throw JsonPatchException("Nothing at $token")
    }
}
