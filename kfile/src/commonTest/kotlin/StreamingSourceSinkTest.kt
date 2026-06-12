import es.jvbabi.kfile.File
import es.jvbabi.kfile.FileOperationOnDirectoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.readByteArray
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StreamingSourceSinkTest : FunSpec({
    test("Write and read using sink/source") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.bin")
        file.exists() shouldBe false

        file.sink().use { sink ->
            sink.write(byteArrayOf(0x01, 0x02, 0x03, 0x04))
            sink.flush()
        }

        file.exists() shouldBe true

        val bytes = file.source().use { source ->
            source.readByteArray(4)
        }

        bytes shouldBe byteArrayOf(0x01, 0x02, 0x03, 0x04)
        file.delete()
    }

    test("Write large data and read back") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.bin")
        val data = ByteArray(32768) { it.toByte() }

        file.sink().use { sink ->
            sink.write(data)
            sink.flush()
        }

        file.source().use { source ->
            val readBack = source.readByteArray(data.size)
            readBack shouldBe data
        }

        file.delete()
    }

    test("Empty file via sink/source") {
        val tmp = File.getTempDirectory()
        val file = tmp.resolve("test-${Uuid.random()}.bin")
        file.writeText("")

        file.source().use { source ->
            source.readByteArray(0) shouldBe byteArrayOf()
        }

        file.delete()
    }

    test("Source on directory throws") {
        val dir = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> {
            dir.source()
        }
    }

    test("Sink on directory throws") {
        val dir = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> {
            dir.sink()
        }
    }
})
