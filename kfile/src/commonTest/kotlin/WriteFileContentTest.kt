import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WriteFileContentTest: FunSpec({
    test("Write content to file") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.txt")
        file.exists() shouldBe false

        val content = (1..5).joinToString("") { Uuid.random().toString() }
        file.writeText(content)
        file.exists() shouldBe true

        file.readText() shouldBe content

        file.delete()

        file.exists() shouldBe false
    }
})