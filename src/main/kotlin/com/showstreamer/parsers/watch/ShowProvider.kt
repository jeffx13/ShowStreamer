package com.showstreamer.parsers.watch

import com.showstreamer.parsers.BaseProvider
import com.showstreamer.parsers.Video

abstract class ShowProvider: BaseProvider<ShowResponse, TvType>() {
    abstract val isRateLimited: Boolean
    abstract suspend fun loadSource(episode: SEpisode): Video?
}