package com.showstreamer.parsers.watch.providers.animeproviders

import com.showstreamer.*
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.setEpisodes
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.utils.*
import kotlinx.coroutines.*
import org.jsoup.nodes.Element

class AnimePahe : ShowProvider() {
    override var hostUrl = "https://animepahe.com"
    override var language = "en"
    override var name = "AnimePahe"
    var lastQuery = ""

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchQueryData(
        @JsonProperty("slug") val slug: String,
        @JsonProperty("title") val title: String,
        @JsonProperty("poster") val poster: String?,
        @JsonProperty("session") val session: String,
        @JsonProperty("year") val year: String,
        @JsonProperty("episodes") val episodes: String,
        @JsonProperty("score") val score: Float,
        @JsonProperty("status") val status: String,

        )
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class SearchQuery(
        @JsonProperty("data") val data: List<SearchQueryData>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ReleaseRouteResponse(
        @JsonProperty("last_page") val last_page: Int,
        @JsonProperty("data") val data: List<ReleaseResponse>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class ReleaseResponse(
            @JsonProperty("episode") val episode: String,
            @JsonProperty("anime_id") val anime_id: Int,
            @JsonProperty("anime_title") val title: String,
            @JsonProperty("snapshot") val snapshot: String,
            @JsonProperty("session") val session: String,
            @JsonProperty("anime_session") val anime_session: String,
        )
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EpisodeData(
        @JsonProperty("episode") val episode: String,
        @JsonProperty("session") val session: String,
        @JsonProperty("filler") val filler: String,
        @JsonProperty("episode2") val episode2: String,
        @JsonProperty("duration") val duration: String,
        @JsonProperty("created_at") val created_at: String,
        @JsonProperty("snapshot") val snapshot: String)

    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse> {
        if(lastQuery==query)return emptyList()
        val resp = app.get("$hostUrl/api?m=search&q=${query.urlEncoded()}&l=8&page=$page")
        if(resp.text.contains("\"from\":null,\"to\":null"))return emptyList()
        return resp.parsed<SearchQuery>().data.map {anime->
            val status = when(anime.status){
                "Currently Airing" -> Status.Ongoing
                "Finished Airing" -> Status.Completed
                else -> Status.Unknown
            }
            ShowResponse(
                anime.title,
                provider = name,
                TvType.Anime,
                anime.session,
                anime.poster,
                anime.episodes,
                anime.year,
                status
            )
        }

    }

    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {
        return latest(page,type)
    }

    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        return app.get("$hostUrl/api?m=airing&page=$page").parsed<ReleaseRouteResponse>().data.map {
            ShowResponse(it.title, provider = name, TvType.Anime, it.anime_session, it.snapshot, it.episode)
        }
    }

    private fun getType(text:String): TvType {
        val type = when(text){
            "TV" -> TvType.Anime
            else -> TvType.Anime
        }
        return type
    }
    private fun getStatus(text:String): Status {
        val status = when(text){
            "Currently Airing" -> Status.Ongoing
            else -> Status.Unknown
        }
        return status
    }
    private fun Element.getInfo(infoType:String)=this.selectFirst("div strong:containsOwn($infoType:)")!!.parent()!!.text().substringAfter("$infoType: ")


    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val episodeTask = GlobalScope.launch{
            val totalPages = app.get("$hostUrl/api?m=release&id=${show.url}&sort=episode_asc&page=1").json.getInt("last_page")
            val pages = (1..totalPages).map {page->
                withContext(Dispatchers.IO) {
                    app.get("$hostUrl/api?m=release&id=${show.url}&sort=episode_asc&page=$page").json.getJSONArray("data").toString().parsed<List<EpisodeData>>().map { episode->
                        SEpisode(show.title, episode.episode, episode.episode.toInt(), episode.session)
                    }
                }
            }
            show.setEpisodes(pages.flatten())
        }
        val document = app.get("$hostUrl/anime/${show.url}").document
        show.title = document.selectText(".title-wrapper h1 span")
        show.posterUrl = document.selectHref(".anime-poster a")
        show.description = document.selectText(".anime-synopsis")
        val info = document.selectFirst("div.col-sm-4.anime-info")!!
        show.year = info.getInfo("Aired")
        //val episodes = info.getInfo("Episodes")
        show.type = getType(info.getInfo("Type"))
        show.genres = info.select(".anime-genre li").map { it.text() }.toMutableList()
        show.status = getStatus(info.getInfo("Status"))
        episodeTask.join()
        return show
    }

    override suspend fun loadSource(episode: SEpisode): Video? {
        val resp = app.get("$hostUrl/api?m=links&id=${episode.url}&p=kwik", mapOf("referer" to "https://www.o8tv.com/")).json.getJSONArray("data")
        val highestQuality = resp.last().toString().json
        val quality = highestQuality.keys().next()
        val kwik = highestQuality.getJSONObject(quality).getString("kwik")
        val response = app.get(kwik, mapOf("referer" to "https://animepahe.com/")).text
        val obfUrl = kwikRe.find(response, 0)?.groupValues?.get(1)
        val i = obfUrl?.split('|')?.reversed()!!
        val m3u8Url = "${i[0]}://${i[1]}-${i[2]}.${i[3]}.${i[4]}.${i[5]}/${i[6]}/${i[7]}/${i[8]}/${i[9]}.${i[10]}"
        return Video(episode.episodeName, episode.showName, m3u8Url, "kwik.cx", true, quality = "1080")
    }

    override var isRateLimited = false
    private val kwikRe = Regex("Plyr\\|(.+?)'")
}
