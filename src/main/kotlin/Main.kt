import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

object Main {

    var key: String = ""
    var path: String = ""

    val logging = false
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

                val itemsByDeathDate = mutableMapOf<Int, MutableList<DeadPerson>>()

                var i = 1
                while (true) {

                    val item = readItem(jParser) ?: continue

                    val name = item.englishName()

                    if (item.isFictionalHuman()) {

                        //println("$name is fictional")
                        continue
                    }

                    if (!item.isHuman()) {

                        //println("$name is not a human")
                        continue
                    }

                    if (!item.hasLifeDates()) {

                        //println("$name doesn't have a known life span")
                        continue
                    }

                    println("${i++}: $name - ${item.englishDescription()}")

                    val deathDate = item.deathDate()
                    val items = itemsByDeathDate.computeIfAbsent(deathDate) { mutableListOf() }
                    items.add(item.toDeadPerson())

                    if ((i % 1000) == 0) {
                        save(itemsByDeathDate)
                    }
                }
           }
        }
    }

    val objectMapper = ObjectMapper().writerWithDefaultPrettyPrinter()

    private fun save(items: Map<Int, List<DeadPerson>>) {
        objectMapper.writeValue(File("items.json"), items)
    }

    private fun readItem(jParser: JsonParser): Item? {
        take(jParser, JsonToken.START_OBJECT)

        var id = "?"
        var name = mapOf<String, String>()
        var description = mapOf<String, String>()
        var claims = mapOf<String, String>()

        do {
            when (val token = nextToken(jParser)) {
                JsonToken.END_OBJECT -> {
                    return Item(id, name, description, claims)
                }
                JsonToken.FIELD_NAME -> {
                    when (val key = jParser.valueAsString) {

                        "type" -> {
                            val typeValue = nextValueAsString(jParser)
                            if (typeValue != "item") {
                                println("Expected 'item' but was '$typeValue'")
                                skipUntil(jParser, JsonToken.END_OBJECT)
                                return null
                            }
                        }

                        "id" -> {
                            id = nextValueAsString(jParser)
                        }

                        "labels" -> {
                            name = nextValueAsLanguageStrings(jParser)
                        }

                        "descriptions" -> {
                            description = nextValueAsLanguageStrings(jParser)
                        }

                        "claims" -> {
                            claims = nextValueAsClaims(jParser)
                        }

                        else -> {
                            logln("Skipping $key")

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


    private fun nextValueAsClaims(jParser: JsonParser): Map<String, String> {
        val claims = mutableMapOf<String,String>()

        expectNextToken(jParser, JsonToken.START_OBJECT)

        while (true) {
            val next = nextToken(jParser)
            if (next == JsonToken.END_OBJECT)
                break

            if (next != JsonToken.FIELD_NAME)
                throw Exception("Expected field")

            val key = jParser.valueAsString
            expectNextToken(jParser, JsonToken.START_ARRAY)
            expectNextToken(jParser, JsonToken.START_OBJECT)

            //println("claim: $key")
            //if (key == Item.INSTANCE_OF)
            //    println()

            expectNextToken(jParser, JsonToken.FIELD_NAME)
            if (jParser.valueAsString == "id") {
                skipValue(jParser)
                expectNextToken(jParser, JsonToken.FIELD_NAME)
            }

            if (jParser.valueAsString != "mainsnak")
                throw Exception("Expected 'mainsnak' but was '${jParser.valueAsString}'")

            val snak = nextValueAsSnak(jParser)
            if (snak != null)
                claims[key] = snak

            skipUntil(jParser, JsonToken.END_OBJECT)
            skipUntil(jParser, JsonToken.END_ARRAY)
        }

        return claims
    }

    private fun nextValueAsSnak(jParser: JsonParser): String? {

        expectNextToken(jParser, JsonToken.START_OBJECT)
        val obj = readObject(jParser)
        val snaktype = obj["snaktype"]
        when (snaktype) {
            "novalue" -> {
                return "(no value)"
            }
            "somevalue" -> {
                return Item.UNKNOWN
            }
            "value" -> {
                val datavalue = obj["datavalue"] as Map<String, Any>
                when (datavalue["type"]) {
                    "time" -> {
                        return asTime(obj)
                    }

                    "wikibase-entityid" -> {
                        return asId(obj)
                    }

                    else -> {
                        return "{}"
                    }
                }
            }
            else -> {
                throw Exception();
            }
        }
    }

    val PROLEPTIC_GREGORIAN = "http://www.wikidata.org/entity/Q1985727"
    val PROLEPTIC_JULIAN = "http://www.wikidata.org/entity/Q1985786"

    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd G")

    private fun asTime(obj: Map<String, Any>): String? {
        val datavalue: Map<String, Any> = obj["datavalue"] as Map<String, Any>
        val value: Map<String, Any> = datavalue["value"] as Map<String, Any>

        val precision = value["precision"] as String
        if (precision != "11")
            return null


        //val timezone = value["timezone"] as String
        //if (timezone != "0")
        //    throw Exception("timezone not 0")

        val calendar = value["calendarmodel"] as String
        val daysOffset = when (calendar) {
            PROLEPTIC_GREGORIAN -> 0
            PROLEPTIC_JULIAN -> 13
            else -> throw Exception("Calendar is $calendar not Gregorian ($PROLEPTIC_GREGORIAN)")
        }

        val time = value["time"] as String
        val eraAD = (time[0] == '+')
        val ymd = time.substring(1, 11)

        val suffix = if (eraAD) "AD" else "BC"
        val datetime = LocalDate.parse("$ymd $suffix", dateFormat).plusDays(daysOffset.toLong())

        return datetime.toEpochDay().toString()
    }

    private fun asId(obj: Map<String, Any>): String {
        val datavalue: Map<String, Any> = obj["datavalue"] as Map<String, Any>
        val value: Map<String, Any> = datavalue["value"] as Map<String, Any>

        return value["id"] as String
    }

    private fun readObject(jParser: JsonParser): Map<String, Any> {
        val obj = mutableMapOf<String,Any>()

        while (true) {
            val next = nextToken(jParser)
            if (next == JsonToken.END_OBJECT)
                break

            if (next != JsonToken.FIELD_NAME)
                throw Exception("Expected field")

            val key = jParser.valueAsString
            val value: Any = when (nextToken(jParser)) {
                JsonToken.START_OBJECT -> {
                    readObject(jParser)
                }
                JsonToken.START_ARRAY -> {
                    readArray(jParser)
                }
                else -> {
                    jParser.valueAsString ?: "null"
                }
            }

            obj[key] = value
        }

        return obj
    }

    private fun readArray(jParser: JsonParser): Array<Any> {
        val arr = mutableListOf<Any>()

        while (true) {
            val next = nextToken(jParser)
            if (next == JsonToken.END_ARRAY)
                break

            arr.add(when (nextToken(jParser)) {
                JsonToken.START_OBJECT -> {
                    readObject(jParser)
                }
                JsonToken.START_ARRAY -> {
                    readArray(jParser)
                }
                else -> {
                    jParser.valueAsString
                }
            })
        }

        return arr.toTypedArray()
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