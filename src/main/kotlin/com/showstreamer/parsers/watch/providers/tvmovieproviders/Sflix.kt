package com.showstreamer.parsers.watch.providers.tvmovieproviders

import com.showstreamer.app
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.utils.*
import java.util.Locale

class Sflix: ShowProvider() {
    override val name = "Sflix.to"
    override val hostUrl = "https://sflix.to"
    override val language = "en"
    override val isRateLimited = false

    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse> {
        val url = "$hostUrl/search/${query.replace(" ", "-")}"
        val document = app.get(url).document
        return document.select("div.flw-item").map {
            val title = it.selectText("h2.film-name")
            val href = fixUrl(it.selectHref("a"))
            val year = it.selectText("span.fdi-item")
            val image = it.select("img").attr("data-src")
            val type = if (href.contains("/movie/")) TvType.Movie else TvType.TvSeries

            val metaInfo = it.select("div.fd-infor > span.fdi-item")
            // val rating = metaInfo[0].text()
            //val quality = getQualityFromString(metaInfo.getOrNull(1)?.text())
            ShowResponse(title, provider = name, type, href, image, null, year)
        }
    }

    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val document = app.get(show.url).document

        val details = document.selectFirst("div.detail_page-watch")!!
        val img = details.selectFirst("img.film-poster-img")
        show.posterUrl = img?.src
        show.title = img?.title ?: throw Exception("No Title")
        show.description = details.select("div.description").text().replace("Overview:", "").trim()
        var duration = document.selectFirst(".fs-item > .duration")?.text()?.trim()
        var year: Int? = null
        var tags: List<String>? = null
        var cast: List<String>? = null
        val youtubeTrailer = document.selectFirst("iframe#iframe-trailer")?.attr("data-src")
        show.ratings = document.selectFirst(".fs-item > .imdb")?.text()?.trim()
            ?.removePrefix("IMDB:")
        document.select("div.elements > .row > div > .row-line").forEach { element ->
            val type = element?.select(".type")?.text() ?: return@forEach
            when {
                type.contains("Released") -> {
                    show.year = Regex("\\d+").find(
                        element.ownText() ?: return@forEach
                    )?.groupValues?.firstOrNull()
                }
                type.contains("Genre") -> {
                    show.genres.addAll(element.select("a").mapNotNull { it.text() })
                }
                type.contains("Cast") -> {
                    cast = element.select("a").mapNotNull { it.text() }
                }
                type.contains("Duration") -> {
                    duration = duration ?: element.ownText().trim()
                }
            }
        }
        val idRegex = Regex(""".*-(\d+)""")
        val dataId = details.attr("data-id")
        val id = if (dataId.isNullOrEmpty())
            idRegex.find(show.url)?.groupValues?.get(1)
                ?: throw Exception("Unable to get id from '${show.url}'")
        else dataId

        if (show.type == TvType.Movie) {
            // Movies
            val episodesUrl = "$hostUrl/ajax/movie/episodes/$id"
            val episodes = app.get(episodesUrl).text
            // Supported streams, they're identical
            val sourceIds = episodes.document.select("a").mapNotNull { element ->
                var sourceId = element.attr("data-id")
                if (sourceId.isNullOrEmpty())
                    sourceId = element.attr("data-linkid")

                if (element.select("span").text().trim().isValidServer()) {
                    if (sourceId.isNullOrEmpty()) {
                        fixUrl(element.attr("href"))
                    } else {
                        "${show.url}.$sourceId".replace("/movie/", "/watch-movie/")
                    }
                } else {
                    null
                }
            }

            val comingSoon = sourceIds.isEmpty()

//            return ShowResponse(title, url, TvType.Movie, sourceIds) {
//                this.year = year
//                this.posterUrl = posterUrl
//                this.plot = plot
//                addDuration(duration)
//                addActors(cast)
//                this.tags = tags
//                this.recommendations = recommendations
//                this.comingSoon = comingSoon
//                addTrailer(youtubeTrailer)
//                this.rating = rating
//            }

        }
        return show
    }

    override suspend fun loadSource(episode: SEpisode): Video? {
        TODO("Not yet implemented")
    }

    fun String?.isValidServer(): Boolean {
        val list = listOf("upcloud", "vidcloud", "streamlare")
        return list.contains(this?.lowercase(Locale.ROOT))
    }
}