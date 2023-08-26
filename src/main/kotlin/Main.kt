import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

object Main {

    var key: String = ""
    var path: String = ""
    val logging = true
    var tab = ""
    val tabSize = 2

    @JvmStatic
    fun main(args: Array<String>) {

        val file = File("C:\\Users\\jason\\Downloads\\wikidata-20220103-all.json.gz")
        FileInputStream(file).use { inStream ->

            GZIPInputStream(inStream).use { gzStream ->

                val jFactory = JsonFactory()
                val jParser: JsonParser = jFactory.createParser(gzStream)

                take(jParser, JsonToken.START_ARRAY)

                while (true) {

                    val item = readItem(jParser)
                }
           }
        }
    }

    private fun readItem(jParser: JsonParser): Item {
        take(jParser, JsonToken.START_OBJECT)

        var id = "?"
        var name = mapOf<String, String>()

        do {
            val token = nextToken(jParser)
            when (token) {
                JsonToken.END_OBJECT -> {
                    return Item(id, name)
                }
                JsonToken.FIELD_NAME -> {
                    when (jParser.valueAsString) {

                        "type" -> {
                            val typeValue = nextValueAsString(jParser)
                            if (typeValue != "item")
                                throw Exception("Expected 'item' but was '$typeValue'")
                        }

                        "id" -> {
                            id = nextValueAsString(jParser)
                        }

                        "labels" -> {
                            name = nextValueAsLanguageStrings(jParser)
                        }

                        "descriptions" -> {
                            name = nextValueAsLanguageStrings(jParser)
                        }

                        else -> {
                            skipValue(jParser)
                        }
                    }
                }
                else -> {
                    throw Exception("Expected field or end of object, got $token")
                }
            }
        } while (true)
    }

    private fun nextFieldAsString(jParser: JsonParser, fieldName: String): String {

        while (true) {

            when (nextToken(jParser)) {
                JsonToken.START_OBJECT -> skipUntil(jParser, JsonToken.END_OBJECT)
                JsonToken.START_ARRAY -> skipUntil(jParser, JsonToken.END_ARRAY)
                JsonToken.FIELD_NAME -> {

                    val thisName = jParser.valueAsString
                    if (thisName == fieldName) {

                        return nextValueAsString(jParser)
                    }
                }
                else -> {}
            }
        }
    }

    private fun skipValue(jParser: JsonParser) {

        when (val nextToken = nextToken(jParser)) {
            JsonToken.START_OBJECT -> skipUntil(jParser, JsonToken.END_OBJECT)
            JsonToken.START_ARRAY -> skipUntil(jParser, JsonToken.END_ARRAY)
            else -> {
                if (!nextToken.isScalarValue)
                    throw Exception("Expected value, but was $nextToken")
            }
        }
    }

    private fun nextValueAsString(jParser: JsonParser): String {
        val value = nextToken(jParser)
        if (value != JsonToken.VALUE_STRING)
            throw Exception("Expected value to be string but was $value")

        return jParser.valueAsString
    }

    private fun nextValueAsLanguageStrings(jParser: JsonParser): Map<String, String> {
        val strings = mutableMapOf<String,String>()

        expectNextToken(jParser, JsonToken.START_OBJECT)

        while (true) {
            val next = nextToken(jParser)
            if (next == JsonToken.END_OBJECT)
                break

            if (next != JsonToken.FIELD_NAME)
                throw Exception("Expected field")

            val key = jParser.valueAsString
            expectNextToken(jParser, JsonToken.START_OBJECT)

            expectNextToken(jParser, JsonToken.FIELD_NAME)
            if (jParser.valueAsString != "language")
                throw Exception("Expected 'language' but was '${jParser.valueAsString}'")
            skipValue(jParser)

            expectNextToken(jParser, JsonToken.FIELD_NAME)
            if (jParser.valueAsString != "value")
                throw Exception("Expected 'value' but was '${jParser.valueAsString}'")

            strings[key] = nextValueAsString(jParser)

            expectNextToken(jParser, JsonToken.END_OBJECT)
        }

        return strings
    }

    private fun expectNextToken(jParser: JsonParser, expectedToken: JsonToken) {

        val nextToken = nextToken(jParser)
        if (nextToken != expectedToken)
            throw Exception("Expected value to be $expectedToken but was $nextToken")
    }

    private fun take(jParser: JsonParser, expectedToken: JsonToken) {
        val token = nextToken(jParser)
        if (token != expectedToken)
            throw Exception("Expected token type of $expectedToken but was $token")
    }

    private fun skipUntil(jParser: JsonParser, token: JsonToken) {
        while (true) {
            val next = nextToken(jParser)
            if (next == token)
                return

            when (next) {
                JsonToken.START_OBJECT -> skipUntil(jParser, JsonToken.END_OBJECT)
                JsonToken.START_ARRAY -> skipUntil(jParser, JsonToken.END_ARRAY)
                else -> {}
            }
        }
    }

    private fun nextToken(jParser: JsonParser): JsonToken {
        val token = jParser.nextToken()!!
        when (token) {
            JsonToken.NOT_AVAILABLE -> logln("n/a")
            JsonToken.START_OBJECT -> { logln("\n$tab{"); tabIn() }
            JsonToken.END_OBJECT -> { tabOut(); logln("$tab}") }
            JsonToken.START_ARRAY -> { logln("\n$tab["); tabIn() }
            JsonToken.END_ARRAY -> { tabOut(); logln("$tab]") }
            JsonToken.FIELD_NAME -> { log("$tab\"${jParser.valueAsString}\":") }
            JsonToken.VALUE_EMBEDDED_OBJECT -> log("\$object")
            JsonToken.VALUE_STRING -> logln("\"${jParser.valueAsString}\"")
            JsonToken.VALUE_NUMBER_INT -> logln("${jParser.valueAsInt}")
            JsonToken.VALUE_NUMBER_FLOAT -> logln("${jParser.valueAsDouble}")
            JsonToken.VALUE_TRUE -> logln("true")
            JsonToken.VALUE_FALSE -> logln("false")
            JsonToken.VALUE_NULL -> logln("null")
        }

        key = if (token == JsonToken.FIELD_NAME)
            jParser.valueAsString;
        else
            ""

        return token
    }

    private fun logln(s: String) {
        if (logging)
            println(s)
    }

    private fun log(s: String) {
        if (logging)
            print(s)
    }

    private fun tabIn() {
        for (i in 1..tabSize) {
            tab += " "
        }

        path += ":$key"

        if (path == ":::claims")
            path += ""
    }

    private fun tabOut() {
        tab = tab.substring(0, tab.length - tabSize)
        path = path.substring(0, path.lastIndexOf(':'))
    }
}