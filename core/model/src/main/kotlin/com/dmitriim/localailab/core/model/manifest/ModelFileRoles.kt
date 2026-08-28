package com.dmitriim.localailab.core.model.manifest

import kotlinx.serialization.Serializable

/** Semantic role of an installed model file. Adapters may define additional roles. */
@Serializable
@JvmInline
value class ModelFileRole(val value: String)

object ModelFileRoles {
    val PRIMARY_MODEL = ModelFileRole("PRIMARY_MODEL")
    val ENCODER = ModelFileRole("ENCODER")
    val DECODER = ModelFileRole("DECODER")
    val JOINER = ModelFileRole("JOINER")
    val MERGED_DECODER = ModelFileRole("MERGED_DECODER")
    val PREPROCESSOR = ModelFileRole("PREPROCESSOR")
    val UNCACHED_DECODER = ModelFileRole("UNCACHED_DECODER")
    val CACHED_DECODER = ModelFileRole("CACHED_DECODER")
    val TOKENS = ModelFileRole("TOKENS")
    val VOCODER = ModelFileRole("VOCODER")
    val CONFIG = ModelFileRole("CONFIG")
    val LICENSE = ModelFileRole("LICENSE")
    val FRONTEND_DATA = ModelFileRole("FRONTEND_DATA")
    val VOCABULARY = ModelFileRole("VOCABULARY")
    val EXTERNAL_DATA = ModelFileRole("EXTERNAL_DATA")
}
