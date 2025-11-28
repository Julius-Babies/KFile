import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ListFilesTest: FunSpec({
    test("List files") {
        val dir = File.getWorkingDirectory().parent!!.resolve("testfiles")
        val files = dir.listFiles()
        files.size shouldBe 1
        files[0].name shouldBe "sample_file"
    }
})