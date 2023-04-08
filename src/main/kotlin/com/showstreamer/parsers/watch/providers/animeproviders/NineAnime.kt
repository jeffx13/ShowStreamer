package com.showstreamer.parsers.watch.providers.animeproviders

import com.showstreamer.*
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.parsers.watch.extractors.Vidstream
import com.showstreamer.utils.*
import com.showstreamer.utils.network.NiceResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class NineAnime : ShowProvider() {
    override var name = "9anime"
    override var hostUrl = "https://9anime.pl"
    override var language = "en"
    override var isRateLimited = false

    init {
        GlobalScope.launch {
            Vidstream.renewKeys("BrohFlow")
        }
    }
    private fun parseAnimes(response: NiceResponse): List<ShowResponse> {
        val document = response.parsed<AjaxResult>().html.document
        return try{
            document.select(".ani.items .item").map {element->
                val posterUrl = element.selectFirst("img")!!.attr("src")
                val url = element.selectHref("a.name")
                //val totalEpisodes = element.selectFirst("ep-status total")?.text()
                val subbedEpisodes=element.selectFirst(".ep-status span")!!.text()
                ShowResponse(
                    element.select("a.name").text(), provider = name, TvType.Anime, "$hostUrl/$url",
                    posterUrl, subbedEpisodes
                )
            }
        }catch (e:Exception){
            println(e)
            emptyList()
        }
    }
    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> = parseAnimes(app.get("$hostUrl/ajax/home/widget/trending?page=$page",headers))
    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> = parseAnimes(app.get("$hostUrl/ajax/home/widget/updated-sub?page=$page",headers))
    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse> {
        try {
            val results=app.get(
                "$hostUrl/filter?keyword=${encode(query)}&vrf=${
                    encodeVrf(
                        encode(query),
                        getKey()
                    )
                }&page=$page"
            ).document.select("#list-items .item").map {
                val img = it.select("img")
                ShowResponse(
                    img.attr("alt"),
                    name,
                    TvType.Anime,
                    fixUrl(it.selectHref(".name.d-title")),
                    img.attr("src"),
                    it.selectText(".left span span")
                ).apply {
                    this.genres = it.select(".genre a").map { it.text() }.toMutableList()
                    this.ratings = it.selectText(".m-item.rated span")
                }
            }
            return results
        }catch (e:Exception){
            println(e)
            println(e.stackTrace)
        }
        return emptyList()
    }
    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val document = app.get(show.url,headers).document
        val fetchEpisodesThread = GlobalScope.launch {
            val dataId=document.selectFirst("#watch-main")!!.attr("data-id")
            val vrf = encodeVrf(dataId,getKey())
            if(!fetchEpisodes("$hostUrl/ajax/episode/list/$dataId?vrf=$vrf",show))throw Exception("Failed to retrieve episodes with keys:${getKey()}")
        }
        val element = document.selectFirst(".info")!!
        show.title = element.selectFirst("h1.title")!!.text()
        show.genres = element.select("div:contains(Genre) > span > a").map { it.text().trim(' ') }.toMutableList()
        show.description = element.selectFirst(".content")!!.text()
        show.status = parseStatus(element.selectFirst("div:containsOwn(Status) > span")!!.text())
        show.year = element.selectFirst("div:containsOwn(Date aired) span")!!.text()
        show.provider =name
        show.updateSchedule = document.selectFirst(".alert.next-episode")?.text()?.trim('×',' ',')','(') ?: ""
        show.ratings = element.selectFirst("div:containsOwn(Scores) span")?.text()
        show.views = element.selectFirst("div:containsOwn(Views) span")!!.text()
        val altName = "Other name(s): "
        element.select("div.alias").firstOrNull()?.ownText()?.let {
            if (it.isBlank().not()) {
                show.description = when {
                    show.description.isNullOrBlank() -> altName + it
                    else -> show.description + "\n\n$altName" + it
                }
            }
        }
        fetchEpisodesThread.join()
        return show
    }
    private suspend fun fetchEpisodes(url:String,show: ShowResponse): Boolean {
        val episodesdata=app.get(url,headers)
        if(episodesdata.text.startsWith("<"))return false
        val document = episodesdata.parsed<AjaxResult>().html.document
        show.setEpisodes(document.select("div.episodes ul > li > a").map {element->
            val epNum = element.attr("data-num")
            val ids = element.attr("data-ids").substringBefore(",")
            val name = element.parent()?.select("span.d-title")?.text().orEmpty()
            val namePrefix = "Episode $epNum"
            val episodeName = if (name.isNotEmpty() && name != namePrefix) {
                "Episode $epNum: $name"
            } else {
                "Episode $epNum"
            }

            SEpisode(
                show.title,
                episodeName,
                epNum.toInt(),
                "$hostUrl/ajax/server/list/$ids?vrf=${encodeVrf(ids,getKey())}"
            )
        })
        return true
    }
    override suspend fun loadSource(episode: SEpisode): Video? {
        val document = app.get(episode.url,headers.plus("X-Requested-With" to "XMLHttpRequest")).parsed<AjaxResult>().html.document
        val servers = parseServersFromElements(document.selectFirst("ul")!!.select("li"))
        val vidstreamId = getServerData(servers.vidstream)
        val mcloudId = getServerData(servers.myCloud)
        if (vidstreamId == null && mcloudId == null) {
            throw Exception("No vidstream nor mcloud")
        }
//        println(vidstreamId)
//        println(mcloudId)
        val src = Vidstream.extract(vidstreamId!!.first) ?: Vidstream.extract(mcloudId!!.first)
        src ?: return null
        return src.apply { hasAutoSkip=true;skipData=vidstreamId.second;name = episode.episodeName;showName = episode.showName;println(url) }
    }

    suspend fun getServerData(sourceID:String?): Pair<String, SkipData?>? {
        if(sourceID==null)return null
        val key = getKey()
        return try {
            val episodeBody = app.get(
                "$hostUrl/ajax/server/$sourceID?vrf=${encodeVrf(sourceID, key)}",
                headers.plus("X-Requested-With" to "XMLHttpRequest")
            ).json.getJSONObject("result")
            var skipData: SkipData?=null
            try{
                episodeBody.getJSONObject("skip_data").let {
                    skipData= SkipData(it.getInt("intro_begin"),
                        it.getInt("intro_end"),
                        it.getInt("outro_begin"),
                        it.getInt("outro_end")
                    )
                    println(skipData)
                }
            }catch (e:Exception){
                println("No Skip Data")
            }
            var url=decodeVrf(episodeBody.getString("url"),key.decipher)
            if(url.contains("vidstream")||url.contains("mcloud"))url=url.substringAfter("/e/").substringAfter("/embed/")
            Pair(url,skipData)
        }catch (e:Exception){
            null
        }
    }
    private fun parseStatus(statusString: String): Status {
        return when (statusString) {
            "Releasing" -> Status.Ongoing
            "Completed" -> Status.Completed
            else -> Status.Unknown
        }
    }
    companion object{
        val headers= mapOf(
            "Accept" to "application/json, text/javascript, */*; q=0.01",
//        "Accept-Encoding" to "gzip, deflate, br",
            "Accept-Language" to "en-GB,en-US;q=0.9,en;q=0.8,zh-CN;q=0.7,zh;q=0.6",
            "Referer" to "https://9anime.id/watch/one-piece-film-red.yqkr1/ep-1preview",
            "sec-ch-ua" to "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Google Chrome\";v=\"102\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to	"Windows",
            "Sec-Fetch-Dest" to	"empty",
            "Sec-Fetch-Mode" to	"cors",
            "Sec-Fetch-Site" to	"same-origin",
            "User-Agent" to USER_AGENT,
//        "X-Requested-With" to "XMLHttpRequest"
        )
        fun encrypt(input: String,key: String): String {
            if (input.any { it.code > 255 }) throw Exception("illegal characters!")
            var output = ""

            for (i in input.indices step 3) {
                val a = intArrayOf(-1, -1, -1, -1)
                a[0] = input[i].code shr 2
                a[1] = (3 and input[i].code) shl 4

                if (input.length > i + 1) {
                    a[1] = a[1] or (input[i + 1].code shr 4)
                    a[2] = (15 and input[i + 1].code) shl 2
                }
                if (input.length > i + 2) {
                    a[2] = a[2] or (input[i + 2].code shr 6)
                    a[3] = 63 and input[i + 2].code
                }
                for (n in a) {
                    if (n == -1) output += "="
                    else {
                        if (n in 0..63) output += key[n]
                    }
                }
            }
            return output
        }
        fun cipher(key:String,input: String): String {
            val arr = IntArray(256) { it }

            var u = 0
            var r: Int
            arr.indices.forEach {
                u = (u + arr[it] + key[it % key.length].code) % 256
                r = arr[it]
                arr[it] = arr[u]
                arr[u] = r
            }
            u = 0
            var c = 0

            return input.indices.map { j ->
                c = (c + 1) % 256
                u = (u + arr[c]) % 256
                r = arr[c]
                arr[c] = arr[u]
                arr[u] = r
                (input[j].code xor arr[(arr[c] + arr[u]) % 256]).toChar()
            }.joinToString("")
        }
        fun encodeVrf(input: String, key: CipherKey): String {
            val ciphered=cipher(key.cipher,input)
            val encrypted=encrypt(ciphered, baseTable )
            val mapped=mapKeys(encrypted,key.keyMap)
            val encryptedAgain=encrypt(mapped, baseTable)
            return encode(encryptedAgain)
        }
        private fun mapKeys(encrypted:String, keyMap:String): String {
            val table =keyMap.split("")
            return encrypted.mapIndexedNotNull { i, c ->
                table.getOrNull((baseTable1.indexOf(c) * 16)+1 + (i % 16))
            }.joinToString("")
        }
        private const val baseTable  ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=_"
        const val baseTable1 ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+=/_"
        data class CipherKey(
            @JsonProperty("cipher") var cipher:String,
            @JsonProperty("decipher") val decipher:String,
            @JsonProperty("keyMap") val keyMap:String)
        fun decodeVrf(input: String, mainKey: String) = decode(cipher(mainKey, decrypt(input)))
        fun decrypt(input: String): String {
            val key="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            val t = if (input.replace("""[\t\n\f\r]""".toRegex(), "").length % 4 == 0) {
                input.replace("""==?$""".toRegex(), "")
            } else input
            if (t.length % 4 == 1 || t.contains("""[^+/A-Za-z0-9]""".toRegex())) throw Exception("bad input")
            var i: Int
            var r = ""
            var e = 0
            var u = 0
//            println(t)
            for (o in t.indices) {
                e = e shl 6
                i = key.indexOf(t[o])

                e = e or i
                u += 6

//                print("${t[o]} ")
                if (24 == u) {

                    r += ((16711680 and e) shr 16).toChar()
//                    print("${((16711680 and e) shr 16)} ")
                    r += ((65280 and e) shr 8).toChar()
//                    print("${(65280 and e) shr 8} ")

                    r += (255 and e).toChar()

//                    print("${(255 and e)} ")


                    e = 0
                    u = 0
                }
            }
//            r.forEach { print(it.code.toString()+" ") }
//            println(t.indices)
            return if (12 == u) {
                e = e shr 4
                r + e.toChar()
            } else {
                if (18 == u) {
                    e = e shr 2
                    r += ((65280 and e) shr 8).toChar()
                    r += (255 and e).toChar()
                }
                r
            }

        }
        fun encode(input: String): String = java.net.URLEncoder.encode(input, "utf-8").replace("+", "%20")
        fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")

        private var lastChecked = 0L

        private const val jsonLink = "https://raw.githubusercontent.com/AnimeJeff/Brohflow/main/keys.json"
        private var cipherKey: CipherKey? = null
        val githubHeaders= mapOf(
            "Authorization" to "token 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",
            "Accept" to "application/vnd.github.v3.raw"
        )
        suspend fun getKey(): CipherKey {

            cipherKey =
                if (cipherKey != null && (lastChecked - System.currentTimeMillis()) < 1000 * 60 * 30) cipherKey!!
                else {
                    lastChecked = System.currentTimeMillis()
                    app.get(jsonLink, githubHeaders) .parsed()//.also{ println(it)}
                }
            return cipherKey!!
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class AjaxResult(@JsonProperty("result") val html:String)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class AjaxHtml(@JsonProperty("html") val html:String)

        data class Servers(
            @JsonProperty("41") var vidstream:String? = null,
            @JsonProperty("28") var myCloud:String? = null,
            @JsonProperty("43") var videoVard:String? = null,
            @JsonProperty("40") var streamtape:String? = null,
            @JsonProperty("35") var mp4upload:String? = null,
            @JsonProperty("44") var filemoon:String? = null)
        val Element.linkID:String
            get() = this.attr("data-link-id")
        fun parseServersFromElements(elements: Elements): Servers {
            val servers = Servers()
            elements.forEach { element ->
                val serverID =element.attr("data-sv-id")
                when(serverID){
                    "41"-> servers.vidstream = element.linkID
                    "28"-> servers.myCloud = element.linkID
                    "43"-> servers.videoVard = element.linkID
                    "40"-> servers.streamtape = element.linkID
                    "35"-> servers.mp4upload = element.linkID
                    "44"-> servers.filemoon = element.linkID
                    else -> throw Exception("New Server!")
                }
            }
            return servers
        }




    }
}

