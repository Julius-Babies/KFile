import es.jvbabi.kfile.File
import es.jvbabi.kfile.FileOperationOnDirectoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GetFileContentTest: FunSpec({
    test("Get content of file") {
        val file = File.getWorkingDirectory().parent!!.resolve("testfiles").resolve("sample_file")
        file.readText() shouldBe SAMPLE_FILE_CONTENT
    }

    test("Try getting content of directory") {
        val file = File.getWorkingDirectory()
        shouldThrow<FileOperationOnDirectoryException> { file.readText() }
    }
})

// Don't modify this, line breaks are important
private const val SAMPLE_FILE_CONTENT = """Quam eum minus alias tenetur reprehenderit cumque ut voluptatum. Quam consectetur minus est quia. Aperiam ut doloribus nam unde commodi laborum ut. Et nesciunt et magnam ut ut. Ad sint odio occaecati ut.

Quis deleniti temporibus voluptatem. Repellendus occaecati qui necessitatibus iure eveniet. Maxime consequatur repellat exercitationem. Aut voluptatibus id pariatur et excepturi laborum autem. A dolorem nostrum voluptatem.

Eaque laborum veritatis eos adipisci. Est at rerum fugit harum ipsam voluptas aspernatur. Sequi ratione esse distinctio at voluptas quia quia. Laborum esse aut aperiam. Veritatis voluptatem dolorum dolorum quia voluptate error veritatis.

Voluptas dolor et eaque voluptas vitae excepturi. Vero sit dolores pariatur itaque cupiditate exercitationem non voluptatem. Inventore fugiat ipsum similique aliquam. Consequatur aperiam nesciunt enim quo temporibus placeat et. Aut illo in dolor nobis non itaque voluptate.

Omnis velit animi aut et. Reiciendis voluptatem deserunt sunt consequatur at unde quas. Omnis vero odit distinctio quibusdam et magni. Eveniet quis magni eligendi est minima ratione sunt libero. Nostrum porro hic consequatur. Qui et unde dolorem eos.
"""