import java.nio.file.Path
import kotlin.io.path.readText

object ChunkJsonReader {
    private val requiredFields = listOf(
        "chunk_id",
        "source",
        "title",
        "section",
        "strategy",
        "text",
        "embedding"
    )

    fun read(path: Path): List<Chunk> {
        val value = JsonParser(path.readText()).parse()
        val items = value as? List<*> ?: error("$path must contain a JSON array.")

        return items.mapIndexed { index, item ->
            val map = item as? Map<*, *> ?: error("Chunk #${index + 1} in $path is not a JSON object.")
            val missing = requiredFields.filterNot { map.containsKey(it) }
            require(missing.isEmpty()) {
                "Chunk #${index + 1} in $path misses required fields: ${missing.joinToString()}"
            }

            Chunk(
                chunkId = map.string("chunk_id"),
                source = map.string("source"),
                title = map.string("title"),
                section = map["section"] as? String,
                strategy = map.string("strategy"),
                text = map.string("text"),
                embedding = map.embedding()
            )
        }
    }

    fun validateMetadata(path: Path): Int {
        return read(path).size
    }

    private fun Map<*, *>.string(key: String): String {
        return this[key] as? String ?: error("Required field '$key' must be a string.")
    }

    private fun Map<*, *>.embedding(): List<Float>? {
        val value = this["embedding"] ?: return null
        val items = value as? List<*> ?: error("Required field 'embedding' must be an array or null.")
        return items.map { item ->
            val number = item as? Number ?: error("Embedding values must be numbers.")
            number.toFloat()
        }
    }
}

private class JsonParser(
    private val input: String
) {
    private var index = 0

    fun parse(): Any? {
        val value = parseValue()
        skipWhitespace()
        require(index == input.length) { "Unexpected JSON trailing content at position $index." }
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        require(index < input.length) { "Unexpected end of JSON." }
        return when (input[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            'n' -> parseNull()
            't' -> parseTrue()
            'f' -> parseFalse()
            else -> parseNumber()
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val map = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek('}')) {
            expect('}')
            return map
        }

        while (true) {
            val key = parseString()
            skipWhitespace()
            expect(':')
            map[key] = parseValue()
            skipWhitespace()
            if (peek('}')) {
                expect('}')
                return map
            }
            expect(',')
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val list = mutableListOf<Any?>()
        skipWhitespace()
        if (peek(']')) {
            expect(']')
            return list
        }

        while (true) {
            list += parseValue()
            skipWhitespace()
            if (peek(']')) {
                expect(']')
                return list
            }
            expect(',')
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < input.length) {
            when (val char = input[index++]) {
                '"' -> return result.toString()
                '\\' -> result.append(parseEscape())
                else -> result.append(char)
            }
        }
        error("Unterminated JSON string.")
    }

    private fun parseEscape(): Char {
        require(index < input.length) { "Unterminated JSON escape." }
        return when (val char = input[index++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                require(index + 4 <= input.length) { "Invalid unicode escape." }
                val code = input.substring(index, index + 4).toInt(16)
                index += 4
                code.toChar()
            }
            else -> error("Unsupported JSON escape: \\$char")
        }
    }

    private fun parseNumber(): Double {
        val start = index
        if (peek('-')) index++
        while (index < input.length && input[index].isDigit()) index++
        if (peek('.')) {
            index++
            while (index < input.length && input[index].isDigit()) index++
        }
        if (index < input.length && (input[index] == 'e' || input[index] == 'E')) {
            index++
            if (index < input.length && (input[index] == '+' || input[index] == '-')) index++
            while (index < input.length && input[index].isDigit()) index++
        }
        require(start < index) { "Expected JSON number at position $index." }
        return input.substring(start, index).toDouble()
    }

    private fun parseNull(): Nothing? {
        expectLiteral("null")
        return null
    }

    private fun parseTrue(): Boolean {
        expectLiteral("true")
        return true
    }

    private fun parseFalse(): Boolean {
        expectLiteral("false")
        return false
    }

    private fun expectLiteral(literal: String) {
        require(input.startsWith(literal, index)) { "Expected '$literal' at position $index." }
        index += literal.length
    }

    private fun expect(char: Char) {
        skipWhitespace()
        require(index < input.length && input[index] == char) { "Expected '$char' at position $index." }
        index++
    }

    private fun peek(char: Char): Boolean {
        return index < input.length && input[index] == char
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].isWhitespace()) index++
    }
}
