package com.dmitriim.localailab.feature.models.impl.data.library

import com.dmitriim.localailab.ai.api.model.manifest.ModelId

/** Returns the stable, filesystem-safe directory name for an installed model. */
internal fun ModelId.storageDirectoryName(): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
