import es.jvbabi.kfile.File
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IsFileDirectoryTest: FunSpec({
    test("Relative file") {
        File("../gradle/libs.versions.toml").isDirectory() shouldBe false
    }

    test("Relative directory") {
        File("../gradle").isDirectory() shouldBe true
    }

    test("Absolute file") {
        File("C:/Windows/System32/explorer.exe").isDirectory() shouldBe false
    }

    test("Absolute directory") {
        File("C:/Windows").isDirectory() shouldBe true
    }
})