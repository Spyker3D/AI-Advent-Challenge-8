import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.relativeTo
import kotlin.io.path.readText

class DocumentLoader(
    private val rootDir: Path,
    private val inputDirs: List<Path> = listOf(
        rootDir.resolve("input/docs"),
        rootDir.resolve("input/project_sources")
    )
) {
    private val allowedExtensions = setOf("md", "txt", "kt", "kts", "gradle", "toml")
    private val ignoredDirectories = setOf("build", ".gradle", ".git", ".idea")

    fun load(): List<LoadedDocument> {
        return inputDirs
            .filter { Files.exists(it) }
            .flatMap { inputDir -> loadFrom(inputDir) }
            .sortedBy { it.source }
    }

    private fun loadFrom(inputDir: Path): List<LoadedDocument> {
        return Files.walk(inputDir).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.extension.lowercase() in allowedExtensions }
                .filter { !hasIgnoredDirectory(it) }
                .map { path ->
                    val source = path.relativeTo(rootDir).toString().replace('\\', '/')
                    LoadedDocument(
                        path = path,
                        source = source,
                        title = path.fileName.toString(),
                        extension = path.extension.lowercase(),
                        text = path.readText()
                    )
                }
                .toList()
        }
    }

    private fun hasIgnoredDirectory(path: Path): Boolean {
        return path
            .map { it.toString() }
            .any { it in ignoredDirectories }
    }
}
