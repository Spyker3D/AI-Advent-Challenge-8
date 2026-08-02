package com.aiassistant.core.data.microfirst

import com.aiassistant.core.domain.inference.IncidentCategory
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AssetMicroPrototypeProviderTest {

    private val gson = Gson()

    @Test
    fun `parses exactly five concrete categories with eight prototypes each`() {
        val prototypes = validPrototypes().toMutableMap().also { values ->
            values[IncidentCategory.NETWORK_UNAVAILABLE.name] =
                values.getValue(IncidentCategory.NETWORK_UNAVAILABLE.name)
                    .mapIndexed { index, value -> if (index == 0) " $value " else value }
        }

        val result = AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(prototypes), gson)

        assertEquals(IncidentCategory.entries.filterNot { it == IncidentCategory.AMBIGUOUS }.toSet(), result.keys)
        assertEquals("NETWORK_UNAVAILABLE-1", result.getValue(IncidentCategory.NETWORK_UNAVAILABLE).first())
    }

    @Test
    fun `rejects category with fewer than eight prototypes`() {
        val prototypes = validPrototypes().toMutableMap().also {
            it[IncidentCategory.OPENAI_TIMEOUT.name] = it.getValue(IncidentCategory.OPENAI_TIMEOUT.name).take(7)
        }

        assertThrows(IllegalArgumentException::class.java) {
            AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(prototypes), gson)
        }
    }

    @Test
    fun `rejects missing concrete category`() {
        val prototypes = validPrototypes().toMutableMap().also {
            it.remove(IncidentCategory.EMPTY_AI_RESPONSE.name)
        }

        assertThrows(IllegalArgumentException::class.java) {
            AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(prototypes), gson)
        }
    }

    @Test
    fun `rejects ambiguous category`() {
        val prototypes = validPrototypes().toMutableMap().also {
            it[IncidentCategory.AMBIGUOUS.name] = prototypeValues(IncidentCategory.AMBIGUOUS)
        }

        assertThrows(IllegalArgumentException::class.java) {
            AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(prototypes), gson)
        }
    }

    @Test
    fun `rejects unknown category and blank prototype`() {
        val unknown = validPrototypes().toMutableMap().also { it["UNKNOWN"] = prototypeValues(IncidentCategory.AMBIGUOUS) }
        assertThrows(IllegalArgumentException::class.java) {
            AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(unknown), gson)
        }

        val blank = validPrototypes().toMutableMap().also {
            it[IncidentCategory.NETWORK_UNAVAILABLE.name] = it.getValue(IncidentCategory.NETWORK_UNAVAILABLE.name)
                .toMutableList().apply { this[0] = " " }
        }
        assertThrows(IllegalArgumentException::class.java) {
            AssetMicroPrototypeProvider.parsePrototypes(gson.toJson(blank), gson)
        }
    }

    private fun validPrototypes(): Map<String, List<String>> = IncidentCategory.entries
        .filterNot { it == IncidentCategory.AMBIGUOUS }
        .associate { it.name to prototypeValues(it) }

    private fun prototypeValues(category: IncidentCategory): List<String> =
        (1..8).map { index -> "${category.name}-$index" }
}
