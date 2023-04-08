package com.showstreamer.parsers.watch.providers.mixedproviders

import com.showstreamer.*
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.setEpisodes
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.utils.*
import org.json.JSONArray
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

class Mudvod : ShowProvider() {
    override var name = "泥巴影院"
    override var hostUrl = "https://www.nivod.tv/"
    override var language = "en"
    override var isRateLimited = false
    private val _HOST_CONFIG_KEY = "2x_Give_it_a_shot"
    private val _bp_app_version = "1.0"
    private val _bp_platform = "3"
    private val _bp_market_id = "web_nivod"
    private val _bp_device_code = "web"
    private val _bp_versioncode = "1"
    private val _QUERY_PREFIX = "__QUERY::"
    private val _BODY_PREFIX = "__BODY::"
    private val _SECRET_PREFIX = "__KEY::"
    private val _oid = "d53355d5b6c3cc51878ffeff1b381210dc81a22901a6bb04"
    private var _mts = "1678639095245"//System.currentTimeMillis().toString()
    private val queryMap: Map<String, String> = mapOf(
        "_ts" to _mts,
        "app_version" to _bp_app_version,
        "device_code" to _bp_device_code,
        "market_id" to _bp_market_id,
        "oid" to _oid,
        "platform" to _bp_platform,
        "versioncode" to _bp_versioncode
    )
    private val mudvodHeaders = mapOf("referer" to "https://www.nivod.tv")
    private val channelId= mapOf(
        TvType.Movie to 1,
        TvType.TvSeries to 2,
        TvType.Reality to 3,
        TvType.Anime to 4,
        TvType.Documentary to 6)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchResultData(
        @JsonProperty("showTitle") val title: String,
        @JsonProperty("actors") val actor: String?,
        @JsonProperty("addDate") val addDate: Long,
        @JsonProperty("channelName") val type: String,
        @JsonProperty("director") val director: String?,
        @JsonProperty("episodesTxt") val episodesTxt: String?,
        @JsonProperty("showImg") val poster: String,
        @JsonProperty("showTypeName") val genre: String,
        @JsonProperty("regionName") val regionName: String,
        //@JsonProperty("favoriteCount") val favoriteCount: Int?,
        @JsonProperty("hot") val hot: String,
        @JsonProperty("rating") val rating: Int?,
        //@JsonProperty("voteUp") val voteUp: Int?,
        //@JsonProperty("voteDown") val voteDown: Int?,
        @JsonProperty("playLangs") val playLangs: List<Map<String, String>>,
        //@JsonProperty("playResolutions") val playResolutions:  List<String>,
        @JsonProperty("postYear") val year: String,
        @JsonProperty("showIdCode") val showIdCode: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ShowDetailsData(
        //@JsonProperty("hot") val hot: String?,
        //@JsonProperty("rating") val rating: Int?,
        @JsonProperty("showDesc") val showDesc: String,
        @JsonProperty("plays") val plays: List<EpisodeData>,
        @JsonProperty("showTitle") val title: String,
        //@JsonProperty("actor") val actor: String,
        @JsonProperty("addDate") val addDate: Long,
        //@JsonProperty("director") val director: String,
        @JsonProperty("shareUrl") val url: String,
        //@JsonProperty("showImg") val poster: String,
        @JsonProperty("postYear") val year: String,
        @JsonProperty("episodesUpdateDesc") val episodesUpdateDesc: String,
        @JsonProperty("episodesTxt") val episodesTxt: String,
        @JsonProperty("showTypeName") val genre: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EpisodeData(
        @JsonProperty("playIdCode") val playIdCode: String,
        @JsonProperty("resolution") val resolution: String,
        //@JsonProperty("createTime") val createTime: Long,
        @JsonProperty("displayName") val displayName: String,
        @JsonProperty("episodeName") val episodeName: String,
        //@JsonProperty("langId") val langId: Int,
        //@JsonProperty("seq") val seq: Int,
    )
    private suspend fun callAPI(url: String, data: Map<String, String>): String  {
        val sign = createSign(queryMap, data)
        val postUrl =
            "$url?_ts=$_mts&app_version=$_bp_app_version&platform=$_bp_platform&market_id=$_bp_market_id&device_code=$_bp_device_code&versioncode=$_bp_versioncode&oid=$_oid&sign=$sign"
        println(data)
        println(postUrl)
        return app.post(postUrl, mudvodHeaders, data = data).text.also { println(it) }.decryptedByDES()


    }
    private fun showsFromJsonArray(showList: JSONArray, type: TvType): List<ShowResponse> {
        return showList.map {
            val result = it.toString().parsed<SearchResultData>()
            var poster = result.poster
            if (!poster.endsWith("jpg")) { poster += "_300x400.jpg" }
            val tvType=when(result.type){
                "电影"-> TvType.Movie
                "电视剧"-> TvType.TvSeries
                "综艺" -> TvType.Reality
                "动漫" -> TvType.Anime
                "纪录片"-> TvType.Documentary
                else -> TvType.Anime
            }
            val status=if(result.episodesTxt?.contains("更新") == true) Status.Ongoing else Status.Completed
            ShowResponse(
                result.title,
                provider = name,
                tvType,
                result.showIdCode,
                poster,
                result.episodesTxt,
                result.year,
                status
            ).apply { genres.add(result.genre)}
                //this.casts.addAll(result.actor.split(','));this.director.add(result.director.toString())
        }.filter { !it.title.contains("假面骑士") || !it.title.contains("奥特曼") }.filter { it.type==type }
    }

    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {
        val data: Map<String, String> = mapOf(
            "sort_by" to "1",
            "channel_id" to "${channelId[type]}",
            "show_type_id" to "0",
            "region_id" to "0",
            "lang_id" to "0",
            "year_range" to " ",
            "start" to "${(page - 1) * 20}"
        )
        val response = callAPI("https://api.nivod.tv/show/filter/WEB/3.2", data).json
        return showsFromJsonArray(response.getJSONArray("list"), type)
    }

    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        val data: Map<String, String> = mapOf(
            "sort_by" to "4",
            "channel_id" to "${channelId[type]}",
            "show_type_id" to "0",
            "region_id" to "0",
            "lang_id" to "0",
            "year_range" to " ",
            "start" to "${(page - 1) * 20}"
        )
        val response = callAPI("https://api.nivod.tv/show/filter/WEB/3.2", data).json
        return showsFromJsonArray(response.getJSONArray("list"), type)
    }

    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse> {
        val data: Map<String, String> = mapOf(
            "keyword" to query,
            "start" to "${(page - 1) * 20}",
            "cat_id" to "1",
            "keyword_type" to "0"
        )
        val response = callAPI("https://api.nivod.tv/show/search/WEB/3.2", data).json

        return showsFromJsonArray(response.getJSONArray("list"), type)
    }

    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        val response = callAPI("https://api.nivod.tv/show/detail/WEB/3.2", mapOf("show_id_code" to show.url)).json
            .getJSONObject("entity").toString()
        val result = response.parsed<ShowDetailsData>()
        val episodes = result.plays.map {data->
            SEpisode(
                show.title,
                data.displayName,
                data.episodeName.filter { it.isDigit() }.toInt(),
                result.url + '&' + data.playIdCode
            )
        }
        show.status = when (result.episodesTxt.contains("集全")) {
            true -> Status.Completed
            false -> Status.Ongoing
        }
        show.apply {
            title = result.title
            this.setEpisodes(episodes)
            this.year = result.year;this.description = result.showDesc
            this.updateSchedule = result.episodesUpdateDesc;this.genres.add(result.genre)
        }
        return show
    }

    override suspend fun loadSource(episode: SEpisode): Video? {
        val showIdCode = episode.url.substringBefore('&')
        val playIdCode = episode.url.substringAfter('&')
        val data: Map<String, String> = mapOf(
            "oid" to "1",
            "play_id_code" to playIdCode,
            "show_id_code" to showIdCode
        )
        val response = callAPI("https://api.nivod.tv/show/play/info/WEB/3.2", data).json
        val playUrl = response.getJSONObject("entity").getString("playUrl")
        return Video(episode.episodeName, episode.showName, playUrl, "mudvod.tv", true, quality = null)
    }


    private fun String.decryptedByDES(): String {
        val key = "diao.com"
        val dks = DESKeySpec(key.toByteArray(StandardCharsets.UTF_8))
        val skf = SecretKeyFactory.getInstance("DES")
        val desKey = skf.generateSecret(dks)
        val cipher = Cipher.getInstance("DES") // DES/ECB/PKCS5Padding for SunJCE
        cipher.init(Cipher.DECRYPT_MODE, desKey)
        val decrypted = cipher.doFinal(this.decodeHex())
        return String(decrypted, StandardCharsets.UTF_8)
    }
    suspend fun test(){

//        val toEncode= "__QUERY::_ts=1678636483631&app_version=1.0&device_code=web&market_id=web_nivod&oid=68940b938c6900b7ca28bff2d92572a5b08a04c3952c206d&platform=3&versioncode=1&__BODY::cat_id=1&keyword=one&keyword_type=0&start=0&__KEY::2x_Give_it_a_shot"
//            val md = MessageDigest.getInstance("MD5")
//        print(BigInteger(1, md.digest(toEncode.toByteArray(StandardCharsets.UTF_8))).toString(16).padStart(32, '0'))
//        val postUrl = "https://api.nivod.tv/show/search/WEB/3.2_ts=1678636483631&app_version=1.0&device_code=web&market_id=web_nivod&oid=68940b938c6900b7ca28bff2d92572a5b08a04c3952c206d&platform=3&versioncode=1&sign=01872dc9e1cfbb733364fd887ead7bca"
//
//        val data: Map<String, String> = mapOf(
//            "keyword" to "one",
//            "start" to "${(1 - 1) * 20}",
//            "cat_id" to "1",
//            "keyword_type" to "0"
//        )
//        println(app.post(postUrl, mudvodHeaders, data = data).text)
    }
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun createSign(queryMap: Map<String, String>, bodyMap: Map<String, String>, secretKey: String = _HOST_CONFIG_KEY): String
    {
        var signQuery = _QUERY_PREFIX
        queryMap.toSortedMap().forEach { entry ->
            signQuery += entry.key + '=' + entry.value + '&'
        }
        var signBody = _BODY_PREFIX
        bodyMap.toSortedMap().forEach { entry ->
            signBody += entry.key + '=' + entry.value.toString() + '&'
        }
        val toEncode = signQuery + signBody + _SECRET_PREFIX + secretKey
        println(toEncode)
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toEncode.toByteArray(StandardCharsets.UTF_8))).toString(16).padStart(32, '0')
    }


}