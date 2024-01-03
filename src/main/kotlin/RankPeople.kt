import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.*


object RankPeople {

    val baseDir = System.getenv("DROPBOX") + "\\Work\\Data\\wikidata"

    class DeadPeople: LinkedHashMap<Int, List<DeadPerson>>()
    {

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val deadPeopleFile = File("$baseDir/dead-people.json")
        val pageRankingFile = File("$baseDir/2023-12-04.allwiki.links.rank.bz2")

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val deadPeople: DeadPeople

        FileInputStream(deadPeopleFile).use { dpf ->

            deadPeople = mapper.readValue(dpf, DeadPeople::class.java)
        }

        println("Loaded ${deadPeople.size} days of dead people")

        val ranks = mutableMapOf<String, Double>()

        FileInputStream(pageRankingFile).use { prf ->

            CompressorStreamFactory().createCompressorInputStream(BufferedInputStream(prf, 4 * 1024 * 1024)).use { input ->

                InputStreamReader(input).use { reader ->

                    BufferedReader(reader).use { br ->

                        while (true) {
                            val line = br.readLine() ?: break

                            val splitter = line.indexOf('\t')
                            if (splitter < 0)
                                throw Exception("Invalid line: $line")

                            val id = line.substring(0, splitter)
                            val rank = line.substring(splitter + 1).toDouble()
                            ranks[id] = rank

                            if (ranks.size % 1000000 == 0)
                                println("Loaded ${ranks.size} ranks of wikidata entries")
                        }
                    }
                }
            }
        }

        println("Loaded ${ranks.size} ranks of wikidata entries")
    }
}