package directory.evnt

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ExamplesTest {
    private val examples = File(System.getProperty("evnt.examples"))

    private fun examplesIn(folder: String) =
        examples.resolve(folder).listFiles { file -> file.name.endsWith(".evnt.json") }.orEmpty().sortedBy { it.name }
            .also { assertTrue(it.isNotEmpty(), "no examples in ${examples.resolve(folder)}") }

    @Test
    fun everyValidExampleParses() {
        val failures = examplesIn("valid").mapNotNull { file ->
            runCatching { OpenEvnt.parse(file.readText()) }.exceptionOrNull()?.let { "${file.name}: ${it.message}" }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    @Test
    fun everyInvalidExampleHasAnIssue() {
        val accepted = examplesIn("invalid").filter { file ->
            OpenEvnt.issuesOf(OpenEvnt.json.parseToJsonElement(file.readText())).isEmpty()
        }
        assertEquals(emptyList(), accepted.map { it.name })
    }

    @Test
    fun validExamplesRoundTrip() {
        for (file in examplesIn("valid")) {
            val event = OpenEvnt.parse(file.readText())
            assertEquals(event, OpenEvnt.parse(event.toJson()), file.name)
        }
    }
}
