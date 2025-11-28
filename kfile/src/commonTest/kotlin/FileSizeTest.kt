import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FileSizeTest: FunSpec({
    test("Get file size") {
        val file = File.getWorkingDirectory().parent!!.resolve("testfiles").resolve("sample_file.txt")
        file.size shouldBe 1194L // File size of sample_file
    }
})