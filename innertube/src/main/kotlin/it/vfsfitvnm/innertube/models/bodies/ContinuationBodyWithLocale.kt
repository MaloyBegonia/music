package com.maloy.innertube.models.bodies

import com.maloy.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ContinuationBodyWithLocale(
    val context: Context = Context.DefaultWebWithLocale,
    val continuation: String,
)
