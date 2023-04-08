//package com.application.parsers.watch.providers.animeproviders
//
//import com.application.app
//import com.application.parsers.providers.*
//import com.application.utils.*
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Element
//import java.net.URI
//
//class Zoro : Provider() {
//    override var isRateLimited: Boolean = false
//    override var providerName:String = "Zoro"
//    override var mainUrl:String = "https://zoro.to"
//    override var lang:String = "en"
//    override suspend fun loadSearchResults(query:String, page:Int, type:ShowType):List<ShowResponse>{
//        val link = "$mainUrl/search?keyword=$query"
//        val document = app.get(link).document
//        return document.select(".flw-item").map {
//            val title = it.selectFirst(".film-detail > .film-name > a")?.attr("title").toString()
//            val filmPoster = it.selectFirst(".film-poster")
//            val poster = filmPoster!!.selectFirst("img")?.attr("data-src")
//
//            val episodes = filmPoster.selectFirst("div.rtl > div.tick-eps")?.text()?.let { eps ->
//                val epRegex = Regex("Ep (\\d+)/")//Regex("Ep (\\d+)/(\\d+)")
//                epRegex.find(eps)?.groupValues?.get(1)?.toIntOrNull()
//            }
//            val href = it.selectFirst(".film-name a")!!.href
//            ShowResponse(title,"$mainUrl$href",ShowType.Anime,poster,episodes.toString())
//
//        }
//    }
//
//    override suspend fun loadPopularResults(page: Int, type:ShowType):List<ShowResponse>{
//        throw NotImplementedError()
//    }
//    override suspend fun loadLatestResults(page: Int, type:ShowType):List<ShowResponse>{
//        throw NotImplementedError()
//    }
//    override suspend fun loadDetails(show: ShowResponse):ShowResponse{
//        val document = app.get(show.url).document
//        show.title = document.selectFirst(".anisc-detail > .film-name")?.text().toString()
//        show.genres = document.select(".anisc-info a[href*=\"/genre/\"]").map { it.text() }.toMutableList()
//        for (info in document.select(".anisc-info > .item.item-title")) {
//            val text = info?.text().toString()
//            when {
//                text.contains("Premiered")->
//                    show.year =
//                        info.selectFirst(".name")?.text().toString().split(" ").last()
//
//                text.contains("Japanese") ->{}
//                    //japaneseTitle = info.selectFirst(".name")?.text().toString()
//
//                text.contains("Status") ->
//                    show.status = getStatus(info.selectFirst(".name")?.text().toString())
//            }
//        }
//        show.description = document.selectFirst(".film-description.m-hide > .text")?.text()
//        val animeId = URI(show.url).path.split("-").last()
//        val episodes = app.get("$mainUrl/ajax/v2/episode/list/$animeId").json["html"].toString().document.select(".ss-list > a[href].ssl-item.ep-item").map {
//            SEpisode(it?.attr("title"),it.selectFirst(".ssli-order")?.text()?.toIntOrNull(),"$mainUrl${it.href}")
//        }
//        show.setEpisodes(episodes)
//
//        val actors = document.select("div.block-actors-content > div.bac-list-wrap > div.bac-item")
//            ?.mapNotNull { head ->
//                val subItems = head.select(".per-info") ?: return@mapNotNull null
//                if (subItems.isEmpty()) return@mapNotNull null
//                var role: ActorRole? = null
//                val mainActor = subItems.first()?.let {
//                    role = when (it.selectFirst(".pi-detail > .pi-cast")?.text()?.trim()) {
//                        "Supporting" -> ActorRole.Supporting
//                        "Main" -> ActorRole.Main
//                        else -> null
//                    }
//                    it.getActor()
//                } ?: return@mapNotNull null
//                val voiceActor = if (subItems.size >= 2) subItems[1]?.getActor() else null
//                ActorData(actor = mainActor, role = role, voiceActor = voiceActor)
//            }
//        println(show)
////        val recommendations =
////            document.select("#main-content > section > .tab-content > div > .film_list-wrap > .flw-item")
////                .mapNotNull { head ->
////                    val filmPoster = head?.selectFirst(".film-poster")
////                    val epPoster = filmPoster?.selectFirst("img")?.attr("data-src")
////                    val a = head?.selectFirst(".film-detail > .film-name > a")
////                    val epHref = a?.href
////                    val epTitle = a?.attr("title")
////                    if (epHref == null || epTitle == null || epPoster == null) {
////                        null
////                    } else {
////                        AnimeSearchResponse(
////                            epTitle,
////                            fixUrl(epHref),
////                            this.name,
////                            mediaType.Anime,
////                            epPoster,
////                            dubStatus = null
////                        )
////                    }
////                }
//
////        return newAnimeLoadResponse(title, url, mediaType.Anime) {
////            japName = japaneseTitle
////            this.year = year
////            addEpisodes(DubStatus.Subbed, episodes)
////            showStatus = status
////            plot = description
////            this.tags = tags
////            //this.recommendations = recommendations
////        }
//        return show
//    }
//    enum class Server{
//        Vidstream,MyCloud,VideoVard,Streamtape,Mp4upload
//    }
//    private fun Element?.getActor(): Actor? {
//        val image =
//            this?.selectFirst(".pi-avatar > img")?.attr("data-src") ?: return null
//        val name = this.selectFirst(".pi-detail > .pi-name")?.text() ?: return null
//        return Actor(name, image)
//    }
//    override suspend fun loadSource(episode: SEpisode):Video{
//        val url ="$mainUrl/ajax/v2/episode/servers?episodeId=" + episode.url.split("=").last()
//        val servers = app.get(url).json["html"].toString().document
//            .select(".server-item[data-type][data-id]").filter{it.attr("data-type")=="sub"}.map {
//                Pair(it.text(),it.attr("data-id"))
//        }
//        servers[0].let {
//            val link = "$mainUrl/ajax/v2/episode/sources?id=${it.second}"
//            //RapidCloud().extract(app.get(link).json["link"].toString())
//        }
//
//        return Video("","","")
//
//    }
//    private fun getStatus(t: String): Status {
//        return when (t) {
//            "Finished Airing" -> Status.Completed
//            "Currently Airing" -> Status.Airing
//            else -> Status.Completed
//        }
//    }
//
//}