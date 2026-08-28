package com.dmitriim.localailab.core.model.manifest

import kotlinx.serialization.Serializable

/** Stable, adapter-owned runtime profile identifier. Values remain extensible across app releases. */
@Serializable
@JvmInline
value class ModelProfileId(val value: String)
