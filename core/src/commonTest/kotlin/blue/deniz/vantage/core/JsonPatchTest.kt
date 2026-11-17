package blue.deniz.vantage.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun json(text: String) = Json.parseToJsonElement(text)

private fun patch(text: String) = json(text) as JsonArray

class JsonPatchTest {
    @Test
    fun theSpecExampleMarksTheNoteDone() {
        val note = json("""{"text":"buy milk","done":false}""")
        val done = note.patched(patch("""[{"op":"test","path":"/done","value":false},{"op":"replace","path":"/done","value":true}]"""))
        assertEquals(json("""{"text":"buy milk","done":true}"""), done)
    }

    @Test
    fun addRemoveMoveAndCopyReachIntoObjectsAndArrays() {
        val start = json("""{"tags":["a","c"],"name":{"en":"Party"}}""")
        val changed = start.patched(
            patch(
                """[
                {"op":"add","path":"/tags/1","value":"b"},
                {"op":"add","path":"/tags/-","value":"d"},
                {"op":"remove","path":"/tags/0"},
                {"op":"copy","from":"/name/en","path":"/name/lt"},
                {"op":"move","from":"/name","path":"/title"}
            ]""",
            ),
        )
        assertEquals(json("""{"tags":["b","c","d"],"title":{"en":"Party","lt":"Party"}}"""), changed)
    }

    @Test
    fun aFailedTestIsToldApartFromOtherFailures() {
        val note = json("""{"done":true}""")
        val tested = assertFailsWith<JsonPatchException> { note.patched(patch("""[{"op":"test","path":"/done","value":false}]""")) }
        assertTrue(tested.isFailedTest)
        val missing = assertFailsWith<JsonPatchException> { note.patched(patch("""[{"op":"replace","path":"/text","value":"x"}]""")) }
        assertTrue(!missing.isFailedTest)
    }

    @Test
    fun pointersEscapeSlashesAndTildes() {
        assertEquals("/a~1b/c~0d", jsonPointerOf("a/b", "c~d"))
        val changed = json("""{"a/b":1}""").patched(patch("""[{"op":"replace","path":"/a~1b","value":2}]"""))
        assertEquals(json("""{"a/b":2}"""), changed)
    }
}
