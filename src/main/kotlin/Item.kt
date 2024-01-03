class Item(
    val id: String,
    val name: Map<String, String>,
    val description: Map<String, String>,
    val claims: Map<String, String>
) {

    companion object {
        val HUMAN = "Q5"
        val FICTIONAL_HUMAN = "Q15632617"
        val INSTANCE_OF = "P31"
        val DATE_OF_BIRTH = "P569"
        val DATE_OF_DEATH = "P570"

        val UNKNOWN = "???"
    }

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

    fun deathDate(): Int {
        return claims[DATE_OF_DEATH]!!.toInt()
    }

    fun hasLifeDates(): Boolean {
        return hasDate(DATE_OF_BIRTH) && hasDate(DATE_OF_DEATH)
    }

    private fun hasDate(claim: String): Boolean {
        val value = claims[claim]
        return !(value == null || value == UNKNOWN || value == "(no value)")
    }

    fun isHuman(): Boolean {
        val i = instanceOf()
        return i == HUMAN
    }

    fun isFictionalHuman(): Boolean {
        val i = instanceOf()
        return i == FICTIONAL_HUMAN
    }

    fun instanceOf(): String {
        return claims[INSTANCE_OF] ?: "??"
    }

    override fun toString(): String {
        return englishName()
    }

    fun toDeadPerson(): DeadPerson {
        return DeadPerson(id, claims[DATE_OF_BIRTH]!!.toInt(), claims[DATE_OF_DEATH]!!.toInt())
    }
}
