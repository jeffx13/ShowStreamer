package com.showstreamer.parsers.watch

import com.showstreamer.parsers.ListType
import com.showstreamer.parsers.Status

enum class TvType{ Anime,TvSeries,Movie,Reality,Documentary }
data class ShowResponse(
    var title: String,
    var provider: String,
    var type: TvType? = null,
    var url: String,
    var posterUrl: String? = null,
    var episodeTxt: String? = null,
    var year: String? = null,
    var status: Status = Status.Completed,
    var updateSchedule: String? = "",
    var description: String? = "",
    var totalEpisodes: Int = 0,
    var genres: MutableList<String> = mutableListOf(),
    var ratings: String? = "",
    var views: String? = "",
    var casts: List<String> = listOf(),
    var director: List<String> = listOf(),
    var episodes: MutableList<SEpisode> = mutableListOf(),
    var listType: ListType = ListType.NONE
)
fun ShowResponse.setEpisodes(episodes:List<SEpisode>){
    this.episodes =episodes.toMutableList()
    this.totalEpisodes=episodes.size
}
