import es.jvbabi.kfile.File
import es.jvbabi.kfile.FileOperationOnDirectoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ByteArrayFileContentTest : FunSpec({
    test("Write and read bytes") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.bin")
        file.exists() shouldBe false

        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte(), 0x7F, 0x80.toByte())
        file.writeBytes(bytes)
        file.exists() shouldBe true

        val readBytes = file.readBytes()
        readBytes shouldBe bytes

        file.delete()
        file.exists() shouldBe false
    }

    test("Write and read empty byte array") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.bin")
        file.writeBytes(byteArrayOf())
        file.readBytes() shouldBe byteArrayOf()
        file.delete()
    }

    test("Try reading bytes from directory") {
        val file = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> { file.readBytes() }
    }

    test("Try writing bytes to directory") {
        val dir = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> { dir.writeBytes(byteArrayOf(1, 2, 3)) }
    }
})
