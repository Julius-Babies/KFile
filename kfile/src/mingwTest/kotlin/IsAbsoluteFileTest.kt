import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class IsAbsoluteFileTest: FunSpec({
    test("Absolute file") {
        File.isPathAbsolute("A:") shouldBe true
    }

    test("Relative file") {
        File.isPathAbsolute("home") shouldBe false
    }

    test("Current directory") {
        File.isPathAbsolute(".") shouldBe false
    }

    test("Parent directory") {
        File.isPathAbsolute("..") shouldBe false
    }
})