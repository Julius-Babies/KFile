import es.jvbabi.kfile.File
import es.jvbabi.kfile.FileOperationOnDirectoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ForEachLineTest : FunSpec({
    test("Iterate over lines") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.writeText("a\nb\nc")
        val lines = mutableListOf<String>()
        file.forEachLine { lines.add(it) }
        lines shouldBe listOf("a", "b", "c")
        file.delete()
    }

    test("Iterate over single line") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.writeText("hello")
        val lines = mutableListOf<String>()
        file.forEachLine { lines.add(it) }
        lines shouldBe listOf("hello")
        file.delete()
    }

    test("Iterate over empty file") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.writeText("")
        val lines = mutableListOf<String>()
        file.forEachLine { lines.add(it) }
        lines shouldBe emptyList()
        file.delete()
    }

    test("ForEachLine on directory throws") {
        val dir = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> {
            dir.forEachLine { }
        }
    }

    test("ReadLines on directory throws") {
        val dir = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> {
            dir.readLines()
        }
    }
})
