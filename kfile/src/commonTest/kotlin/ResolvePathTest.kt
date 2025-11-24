import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ResolvePathTest: FunSpec({
    test("Resolve path") {
        val current = File.getWorkingDirectory()
        val parent1 = current.resolve("../")
        val parent2 = current.parent

        parent2.shouldNotBeNull()
        parent1.absolutePath shouldBe parent2.absolutePath
    }
})