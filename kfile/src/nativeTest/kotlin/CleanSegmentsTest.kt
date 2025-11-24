import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CleanSegmentsTest : FunSpec({
    test("Normal path") {
        val currentDir = File.getWorkingDirectory()

        val file = File("./Data/../Data1/Data2/../../")

        val currentDirSegments = currentDir.cleanSegments
        val fileSegments = file.cleanSegments

        fileSegments.size shouldBe currentDirSegments.size

        currentDirSegments.forEachIndexed { i, currentDirSegment ->
            currentDirSegment shouldBe fileSegments[i]
        }
    }
})