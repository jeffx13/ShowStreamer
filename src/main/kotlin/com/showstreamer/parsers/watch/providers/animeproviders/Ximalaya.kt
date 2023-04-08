package com.showstreamer.parsers.watch.providers.animeproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.app
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.utils.urlDecoded
import com.showstreamer.utils.urlEncoded
import org.json.JSONObject
import org.json.JSONPropertyIgnore

class Ximalaya: ShowProvider() {
    override val isRateLimited: Boolean
        get() = TODO("Not yet implemented")

    override suspend fun loadSource(episode: SEpisode): Video? {
        TODO("Not yet implemented")
    }

    override val name: String
        get() = TODO("Not yet implemented")
    override val hostUrl: String
        get() = TODO("Not yet implemented")

    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse> {
        val params = mapOf(
            "rows" to "5",
            "kw" to query.urlEncoded(),
            "core" to "all",
            "page" to "$page"
        )
        val headers= mapOf(
            "referer" to  "https to //m.ximalaya.com/?source=pc_jump",
            "user-agent" to  "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Mobile Safari/537.36",
        )
        val albums=app.get("https://m.ximalaya.com/m-revision/page/search",headers, params=params)
            .json.getJSONObject("data").getJSONObject("albumViews").getJSONArray("albums").map {it as JSONObject
                val albumInfo = it.getJSONObject("albumInfo")
                val pageUriInfo = it.getJSONObject("pageUriInfo")
                ShowResponse(albumInfo.getString("title"),"Ximalaya",TvType.Anime,pageUriInfo.getString("url").substringAfterLast("/"),albumInfo.getString("cover_path"))
            }
//        println(albums)
        return albums
    }

    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val episodeRegex=Regex("(?<=第)\\d+")
        val url = "http://mobile.ximalaya.com/mobile/v1/album/track/ts-1662583147870?albumId=21981646&device=android&isAsc=true&isQueryInvitationBrand=true&pageId=14&pageSize=20&pre_page=13"

        val data=app.get(url).json.getJSONObject("data")
        val trackList = data.getJSONArray("list").map {it as JSONObject
            val title=it.getString("title")

            SEpisode(it.getString("albumTitle"),title,episodeRegex.find(title)!!.value.toInt(),it.getString("trackId"))
        }
        println()
        return show
    }
//    data class audioDetails(
//        @JsonProperty("")
//    )

}