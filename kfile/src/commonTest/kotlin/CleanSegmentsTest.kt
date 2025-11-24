import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CleanSegmentsTest : FunSpec({
    test("Normal path") {
        val currentDir = File.getWorkingDirectory()

        val file = File("./Data/../Data1/Data2/../../")

        currentDir.absolutePath shouldBe file.absolutePath
    }
})