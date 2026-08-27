package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFormat

internal fun sherpaSttImport(
    displayName: String,
    vararg files: Pair<ModelFileRole, String>,
) = ModelImportDefinition(
    displayName = displayName,
    format = ModelFormat.ONNX,
    files = files.map { (role, path) -> ModelImportFileDefinition(role, relativePath = path) },
)
