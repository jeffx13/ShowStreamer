//package com.application.parsers.watch.providers.mixedproviders//package com.application.parsers.watch.providers.animeproviders
//
//import com.application.app
//import com.application.parsers.providers.*
//import com.application.utils.*
//import org.json.JSONObject
//
//import java.net.URLDecoder
//
//class Tangrenjie : Provider() {
//    override var providerName = "唐人街影院"
//    override var mainUrl = "https://tangrenjie.tv"
//    override var lang = "en"
//    override var isRateLimited = false
//    val channelMap = mapOf(TvType.Movie to 1,
//        TvType.TvSeries to 2,
//        TvType.Reality to 3,
//        TvType.Anime to 4,)
//    override suspend fun loadPopularResults(page: Int, type:TvType): List<ShowResponse> {
//        val document = app.get("$mainUrl/vod/show/by/hits/id/${channelMap[type]}.html").document
//        val ul = document.getElementsByClass("vodlist vodlist_wi author*qq3626/95/000 clearfix")
//        return ul.select("li").map {item->
//            val anchor = item.selectFirst("a.vodlist_thumb.lazyload")!!
//            val title = anchor.attr("title")
//            val href= "$mainUrl/${anchor.href}"
//            val poster = item.selectFirst("a.vodlist_thumb.lazyload")!!.attr("data-original")
//            val episodesName = item.selectFirst("span.pic_text.text_right")!!.text()
//            ShowResponse(title,href,"$mainUrl/$poster",type,null,null,episodesName,Status.Unknown)
//        }
//    }
//    override suspend fun loadLatestResults(page: Int, type:TvType): List<ShowResponse> {
//        val url = "$mainUrl/vod/show/by/time/id/${channelMap[type]}.html"
//        val document = app.get(url).document
//        val ul = document.getElementsByClass("vodlist vodlist_wi author*qq3626/95/000 clearfix")
//        return ul.select("li").map {item->
//            val anchor = item.selectFirst("a.vodlist_thumb.lazyload")!!
//            val title = anchor.attr("title")
//            val href= "$mainUrl/${anchor.href}"
//            val poster = item.selectFirst("a.vodlist_thumb.lazyload")!!.attr("data-original")
//            val episodesName = item.selectFirst("span.pic_text.text_right")!!.text()
//            ShowResponse(title,href,type,"$mainUrl/$poster",null,null,episodesName,Status.Unknown)
//        }
//    }
//
//    override suspend fun loadSearchResults(query: String,page: Int,type:TvType): List<ShowResponse> {
//        val url = "$mainUrl/vod/search/page/$page/wd/$query.html"
//        val document = app.get(url).document
//        return document.select("li.searchlist_item").map {item->
//            val anchor = item.selectFirst("h4.vodlist_title a")!!
//            val title = anchor.attr("title")
//            val href= "$mainUrl/${anchor.href}"
//            val poster = item.selectFirst("a.vodlist_thumb.lazyload")!!.attr("data-original")
//            val episodesName = item.selectFirst("span.pic_text.text_right")!!.text()
//            val type = when(item.selectFirst("span.info_right")!!.text()){
//                "动漫" -> ShowType.Anime
//                "综艺"-> ShowType.Reality
//                "电视剧"-> ShowType.TvSeries
//                "电影"-> ShowType.Movie
//                else -> null
//            }
//            ShowResponse(title,href,"$mainUrl/$poster",type,null,null,episodesName,Status.Unknown)
//        }
//    }
//
//    override suspend fun loadSource(episode: SEpisode): Video {
//        val url = parse_player_aaaa(app.get(episode.url).text).urlDecoded()
//        return Video(url, null,"tangrenjie.tv",true)
//    }
//
//    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
//        val document=app.get(show.url).document
//        val playlist = document.selectFirst("ul.content_playlist.list_scroll.clearfix")
//        val episodes = playlist!!.select(" li a").map{episode->
//            SEpisode(episode.text(),episode.text().filter { it.isDigit() }.toIntOrNull(),"$mainUrl${episode.href}") }
//        val year = document.selectFirst("div.content_detail.content_min.fl > ul > li:nth-child(5) > a")!!.text().toInt()
//        val description = document.selectFirst("div.content font")!!.text()
//        return ShowDetails(show.title,episodes,show.posterUrl,year,episodes.size,description,Status.Unknown)
//    }
//}