import es.jvbabi.kfile.DirectoryOperationOnFileException
import es.jvbabi.kfile.File
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ListFilesTest: FunSpec({
    test("List files") {
        val dir = File.getWorkingDirectory().parent!!.resolve("testfiles")
        val files = dir.listFiles()
        files.size shouldBe 1
        files.first().name shouldBe "sample_file.txt"
        files.first().nameWithoutExtension shouldBe "sample_file"
        files.first().extension shouldBe "txt"
    }

    test("List files on file (exception)") {
        val file = File.getWorkingDirectory().parent!!.resolve("testfiles").resolve("sample_file.txt")
        shouldThrow<DirectoryOperationOnFileException> { file.listFiles() }
    }
})