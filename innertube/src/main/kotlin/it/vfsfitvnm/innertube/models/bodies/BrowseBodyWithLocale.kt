package com.maloy.innertube.models.bodies

import com.maloy.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBodyWithLocale(
    val context: Context = Context.DefaultWebWithLocale,
    val browseId: String,
    val params: String? = null
)
