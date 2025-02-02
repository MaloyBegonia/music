package com.maloy.innertube.models.bodies

import com.maloy.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class NextBody(
    val context: Context = Context.DefaultWeb,
    val videoId: String?,
    val isAudioOnly: Boolean = true,
    val playlistId: String? = null,
    val tunerSettingValue: String = "AUTOMIX_SETTING_NORMAL",
    val index: Int? = null,
    val params: String? = null,
    val playlistSetVideoId: String? = null,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs = WatchEndpointMusicSupportedConfigs(
        musicVideoType = "MUSIC_VIDEO_TYPE_ATV"
        //musicVideoType = "MUSIC_VIDEO_TYPE_OMV"
    )
) {
    @Serializable
    data class WatchEndpointMusicSupportedConfigs(
        val musicVideoType: String
    )
}
