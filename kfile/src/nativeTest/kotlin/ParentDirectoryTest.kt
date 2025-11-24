import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ParentDirectoryTest : FunSpec({
    test("Get parent directory") {
        val currentDir = File.getWorkingDirectory()
        val parent = currentDir.parent
        val parent2 = File(currentDir.absolutePath + "/..")

        parent.shouldNotBeNull()
        parent.cleanSegments shouldBe parent2.cleanSegments
    }
})