package com.maloy.innertube.models.bodies

import com.maloy.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context = Context.DefaultAndroid,
    //val context: Context = Context.DefaultWeb,
    val videoId: String,
    val playlistId: String? = null
)
