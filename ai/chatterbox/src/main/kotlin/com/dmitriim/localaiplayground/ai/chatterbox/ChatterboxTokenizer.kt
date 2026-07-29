package com.dmitriim.localaiplayground.ai.chatterbox

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Minimal, pinned GPT-2 byte-level BPE implementation for Chatterbox's tokenizer.json. */
internal class ChatterboxTokenizer(tokenizerFile: File) {
    private val root = Json.parseToJsonElement(tokenizerFile.readText()).jsonObject
    private val vocab = root.getValue("model").jsonObject.getValue("vocab").jsonObject
        .mapValues { it.value.jsonPrimitive.content.toLong() }
    private val mergeRanks = root.getValue("model").jsonObject.getValue("merges").jsonArray
        .mapIndexed { rank, value ->
            val pair = value.jsonArray
            PairKey(pair[0].jsonPrimitive.content, pair[1].jsonPrimitive.content) to rank
        }
        .toMap()
    private val addedTokens = root.getValue("added_tokens").jsonArray.associate { value ->
        val token = value.jsonObject
        token.getValue("content").jsonPrimitive.content to token.getValue("id").jsonPrimitive.content.toLong()
    }
    private val specialPattern = Pattern.compile(
        addedTokens.keys
            .filterNot { it == END_OF_TEXT }
            .sortedByDescending(String::length)
            .joinToString("|", "(?:", ")") { Pattern.quote(it) },
    )
    private val cache = ConcurrentHashMap<String, LongArray>()

    fun encode(text: String): LongArray {
        require(text.isNotBlank()) { "Enter text to synthesize." }
        val ids = ArrayList<Long>()
        var cursor = 0
        val specialMatcher = specialPattern.matcher(text)
        while (specialMatcher.find()) {
            encodeOrdinary(text.substring(cursor, specialMatcher.start()), ids)
            ids += requireNotNull(addedTokens[specialMatcher.group()])
            cursor = specialMatcher.end()
        }
        encodeOrdinary(text.substring(cursor), ids)
        ids += END_OF_TEXT_ID
        ids += END_OF_TEXT_ID
        require(ids.size <= MAX_TEXT_TOKENS) {
            "Text is too long for Chatterbox ($MAX_TEXT_TOKENS tokenizer tokens maximum)."
        }
        return ids.toLongArray()
    }

    private fun encodeOrdinary(text: String, output: MutableList<Long>) {
        val matcher = GPT2_PATTERN.matcher(text)
        while (matcher.find()) {
            val byteEncoded = buildString {
                matcher.group().toByteArray(Charsets.UTF_8).forEach { byte ->
                    append(BYTE_ENCODER[byte.toInt() and 0xff])
                }
            }
            bpe(byteEncoded).forEach(output::add)
        }
    }

    private fun bpe(token: String): LongArray = cache.getOrPut(token) {
        var pieces = token.codePoints().toArray().map { String(Character.toChars(it)) }
        while (pieces.size > 1) {
            var bestIndex = -1
            var bestRank = Int.MAX_VALUE
            for (index in 0 until pieces.lastIndex) {
                val rank = mergeRanks[PairKey(pieces[index], pieces[index + 1])] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestIndex = index
                }
            }
            if (bestIndex < 0) break
            val selected = PairKey(pieces[bestIndex], pieces[bestIndex + 1])
            val merged = ArrayList<String>(pieces.size)
            var index = 0
            while (index < pieces.size) {
                if (
                    index < pieces.lastIndex &&
                    pieces[index] == selected.first &&
                    pieces[index + 1] == selected.second
                ) {
                    merged += pieces[index] + pieces[index + 1]
                    index += 2
                } else {
                    merged += pieces[index++]
                }
            }
            pieces = merged
        }
        pieces.map { piece ->
            requireNotNull(vocab[piece]) { "Tokenizer vocabulary is missing a byte-level token." }
        }.toLongArray()
    }

    private data class PairKey(val first: String, val second: String)

    private companion object {
        const val END_OF_TEXT = "<|endoftext|>"
        const val END_OF_TEXT_ID = 50_256L
        const val MAX_TEXT_TOKENS = 1_024
        val GPT2_PATTERN: Pattern = Pattern.compile(
            """'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""",
        )
        val BYTE_ENCODER: Array<String> = run {
            val visible = (33..126) + (161..172) + (174..255)
            val values = visible.toMutableList()
            var extra = 0
            for (byte in 0..255) {
                if (byte !in visible) values += 256 + extra++
            }
            Array(256) { byte ->
                val index = if (byte in visible) visible.indexOf(byte) else {
                    visible.size + (0 until byte).count { it !in visible }
                }
                String(Character.toChars(values[index]))
            }
        }
    }
}
