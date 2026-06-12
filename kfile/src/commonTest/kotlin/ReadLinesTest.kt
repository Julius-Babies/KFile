import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ReadLinesTest : FunSpec({
    test("Read all lines from a file") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        val content = "line1\nline2\nline3\n"
        file.writeText(content)
        file.readLines() shouldBe listOf("line1", "line2", "line3")
        file.delete()
    }

    test("Read lines with empty lines") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        val content = "line1\n\nline3\n"
        file.writeText(content)
        file.readLines() shouldBe listOf("line1", "", "line3")
        file.delete()
    }

    test("Read lines from single-line file without trailing newline") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.writeText("only line")
        file.readLines() shouldBe listOf("only line")
        file.delete()
    }

    test("Read lines from empty file") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.writeText("")
        file.readLines() shouldBe emptyList()
        file.delete()
    }
})
