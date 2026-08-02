package com.aiassistant.core.data.microfirst

import android.content.Context
import com.aiassistant.core.domain.inference.IncidentCategory
import com.aiassistant.core.domain.microfirst.MicroPrototypeProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetMicroPrototypeProvider @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : MicroPrototypeProvider {

    override suspend fun loadPrototypes(): Result<Map<IncidentCategory, List<String>>> =
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
                Result.success(parsePrototypes(json, gson))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Result.failure(IllegalStateException("Micro prototypes could not be loaded.", throwable))
            }
        }

    internal companion object {
        const val ASSET_NAME = "prototypes.json"

        fun parsePrototypes(json: String, gson: Gson): Map<IncidentCategory, List<String>> {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val decoded = gson.fromJson<Map<String, List<String>>>(json, type)
                ?: error("Micro prototype asset is empty.")
            require(decoded.keys == REQUIRED_CATEGORY_NAMES) {
                "Micro prototype asset must contain exactly the supported concrete categories."
            }
            return decoded.mapKeys { (key, _) -> IncidentCategory.valueOf(key) }
                .mapValues { (_, values) ->
                    values.map(String::trim).also { prototypes ->
                        require(prototypes.size >= MIN_PROTOTYPES_PER_CATEGORY && prototypes.none(String::isBlank)) {
                            "Each micro prototype category must contain at least $MIN_PROTOTYPES_PER_CATEGORY nonblank prototypes."
                        }
                    }
                }
        }

        private const val MIN_PROTOTYPES_PER_CATEGORY = 8
        private val REQUIRED_CATEGORY_NAMES = IncidentCategory.entries
            .filterNot { it == IncidentCategory.AMBIGUOUS }
            .mapTo(linkedSetOf()) { it.name }
    }
}
