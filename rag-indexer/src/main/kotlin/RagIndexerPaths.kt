import java.nio.file.Path
import java.nio.file.Paths

object RagIndexerPaths {
    fun rootDir(): Path {
        val workingDir = Paths.get("").toAbsolutePath().normalize()
        return if (workingDir.fileName.toString() == "rag-indexer") {
            workingDir
        } else {
            workingDir.resolve("rag-indexer").normalize()
        }
    }
}
