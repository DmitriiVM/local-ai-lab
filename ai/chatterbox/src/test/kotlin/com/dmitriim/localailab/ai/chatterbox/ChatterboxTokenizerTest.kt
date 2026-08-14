package com.dmitriim.localailab.ai.chatterbox

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ChatterboxTokenizerTest {
    @Test
    fun encodesPlainTextWithTerminalTokens() = withTokenizer { tokenizer ->
        assertArrayEquals(longArrayOf(1, 2, 50_256, 50_256), tokenizer.encode("hi"))
    }

    @Test
    fun preservesConfiguredSpecialTokens() = withTokenizer { tokenizer ->
        assertArrayEquals(longArrayOf(1, 50_257, 2, 50_256, 50_256), tokenizer.encode("h<|custom|>i"))
    }

    private inline fun withTokenizer(block: (ChatterboxTokenizer) -> Unit) {
        val file = Files.createTempFile("chatterbox-tokenizer", ".json").toFile()
        try {
            file.writeText(TOKENIZER_JSON)
            block(ChatterboxTokenizer(file))
        } finally {
            file.delete()
        }
    }

    private companion object {
        val TOKENIZER_JSON =
            """
            {
              "model": {
                "vocab": {"h": 1, "i": 2},
                "merges": []
              },
              "added_tokens": [
                {"content": "<|endoftext|>", "id": 50256},
                {"content": "<|custom|>", "id": 50257}
              ]
            }
            """.trimIndent()
    }
}
