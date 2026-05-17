import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CopyFileTest: FunSpec({
    test("Copy file") {
        val source = File.getWorkingDirectory().resolve("testfiles").resolve("sample_file.txt")
        val destination = File.getWorkingDirectory().resolve("testfiles").resolve("sample_file_copy.txt")

        source.copy(destination)

        destination.size shouldBe source.size
        destination.readText() shouldBe source.readText()
    }
})