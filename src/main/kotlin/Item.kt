class Item(
    val id: String,
    val name: Map<String, String>,
    val description: Map<String, String>,
    val claims: Map<String, String>
) {
    private val DOB_PROPERTY = "P569"
    private val DOD_PROPERTY = "P570"

    fun englishName(): String {

        if (name.containsKey("en-gb"))
            return name["en-gb"]!!

        if (name.containsKey("en"))
            return name["en"]!!

        return "??"
    }

    fun englishDescription(): String {

        if (description.containsKey("en-gb"))
            return description["en-gb"]!!

        if (description.containsKey("en"))
            return description["en"]!!

        return "??"
    }

    fun hasLifeDates(): Boolean {
        return claims.containsKey(DOB_PROPERTY) || claims.containsKey(DOD_PROPERTY)
    }
}
