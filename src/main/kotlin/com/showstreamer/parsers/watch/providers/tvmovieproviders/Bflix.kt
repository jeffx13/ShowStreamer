package com.showstreamer.parsers.watch.providers.tvmovieproviders

import com.showstreamer.*
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.setEpisodes
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime
import com.showstreamer.utils.*
import org.jsoup.nodes.Element
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime.Companion.AjaxHtml
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime.Companion.decodeVrf

class Bflix: ShowProvider() {
    override var name:String = "Bflix"
    override var hostUrl:String = "https://bflix.ru"
    override var language:String = "en"
    override var isRateLimited= false
    val mainKey = "DZmuZuXqa9O0z3b7"
    private fun parseResults(document:Element): List<ShowResponse> {
        return document.select("div.item").map {
            val title = it.selectFirst("h3 a")!!.text()
            val href = fixUrl(it.selectHref("a"))
            val image = it.selectFirst("a.poster img")!!.src
            val tvType = if(href.contains("/movie/")) TvType.Movie else TvType.TvSeries
            val qualityInfo = it.selectFirst("div.quality")!!.text()
            val episodeTxt = if(tvType== TvType.TvSeries) it.selectText("div.info span") else qualityInfo
            ShowResponse(title, provider = name, tvType, href, image, episodeTxt)
        }
    }
    override suspend fun search(query:String, page:Int, type: TvType): List<ShowResponse> {
        return parseResults(app.get("$hostUrl/search?keyword=$query&vrf=${encodeVrf(query, mainKey)}&page=$page").document)
    }

    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {
        return parseResults(app.get("$hostUrl/filter?type%5B0%5D=${when(type){
            TvType.Movie->"movies" else->"tv-series"}}&sort=views%3Adesc&page=$page").document)
    }
    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        //println(app.get("$mainUrl/${when(type){mediaType.Movie->"movies" else->"tv-series"}}?page=$page").text)
        return parseResults(app.get("$hostUrl/${when(type){
            TvType.Movie->"movies" else->"tv-series"}}?page=$page").document)
    }
    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val document = app.get(show.url).document
        val movieid = document.selectFirst("div#watch")!!.attr("data-id")
        val movieidencoded = encodeVrf(movieid, mainKey)
        show.description = document.selectFirst(".info .desc")?.text()?.trim()
        show.genres =document.select("div.info .meta div:contains(Genre) a").map { it.text() }.toMutableList()
        val vrfUrl = "$hostUrl/ajax/film/servers?id=$movieid&vrf=$movieidencoded"
        show.year = document.selectText("span[itemprop=dateCreated]")
        val rating = document.selectText("span.imdb")
        println(vrfUrl)
        val episodes = app.get(vrfUrl).parsed<AjaxHtml>().html.document.select("div.episode").map {
            val a = it.selectFirst("a")
            val episodeNumber:Int
            val episodeName:String
            var kname: List<String>
            a!!.attr("data-kname").let { str ->
                if(str.contains("-") && show.type== TvType.TvSeries){
                    kname = str.split("-")
                    val ep = (kname[1].toIntOrNull() ?: 0)
                    episodeName = "S${kname.first()} E${ep}: "
                    episodeNumber=kname.first().toInt()*100 + ep
                }else{
                    episodeName=str.substringBefore('-')
                    episodeNumber=1
                }
            }

            val eptitle = it.selectFirst(".episode a span.name")!!.text()
            val name = episodeName+eptitle
            SEpisode(show.title, name, episodeNumber, a.attr("data-ep"))
//            val secondtitle = it.selectFirst(".episode a span")!!.text()
//                .replace(Regex("(Episode (\\d+):|Episode (\\d+)-|Episode (\\d+))"), "") ?: ""
        }
        show.setEpisodes(episodes)
        return show
    }
    fun encodeVrf(text: String, mainKey: String): String {
        println(text)
        println(mainKey)
        println(NineAnime.encode(NineAnime.encrypt(NineAnime.cipher(mainKey, NineAnime.encode(text)), NineAnime.baseTable1
            ).replace("""=+$""".toRegex(), "")))

        return NineAnime.encode(
            NineAnime.encrypt(
                NineAnime.cipher(mainKey, NineAnime.encode(text)),
                NineAnime.baseTable1
            ).replace("""=+$""".toRegex(), "")
        )
    }
    override suspend fun loadSource(episode: SEpisode): Video? {
       val servers = episode.url.parsed<NineAnime.Companion.Servers>()
        val vidstreamId = getID(servers.vidstream)?.substringBetween("/e/","?")
        val mcloudId = getID(servers.myCloud)?.substringBetween("/e/","?")
        if (vidstreamId == null && mcloudId == null) {
            println(servers)
            throw Exception("No vidstream nor mcloud")
        }
//        val src = vizExtractor.extract(mcloudId) ?: vizExtractor.extract(vidstreamId)
//        println(src)
//        return src ?: throw Exception("No sources found")
        return null
    }
    private suspend fun getID(serverKey:String?): String? {
        return if(serverKey!=null) decodeVrf(app.get("$hostUrl/ajax/episode/info?id=$serverKey").json.getString("url"),mainKey) else null
    }
}