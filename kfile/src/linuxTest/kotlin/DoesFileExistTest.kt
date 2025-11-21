import es.jvbabi.kfile.File
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalKotest::class)
class DoesFileExistTest: FunSpec({
    test("Existing absolute file") {
        File("/usr/bin").exists() shouldBe true
    }

    test("Existing relative file") {
        // runs in kfile module
        File("../gradle/libs.versions.toml").exists() shouldBe true
    }

    test("Not existing absolute file") {
        File("/thisfiledoesnotexist").exists() shouldBe false
    }

    test("Not existing relative file") {
        File("./thisfiledoesnotexist").exists() shouldBe false
    }
})