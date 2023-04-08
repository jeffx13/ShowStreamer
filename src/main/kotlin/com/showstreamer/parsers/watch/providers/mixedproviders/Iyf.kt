package com.showstreamer.parsers.watch.providers.mixedproviders
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.*
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.setEpisodes
import com.showstreamer.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

class Iyf: ShowProvider(){
    override var language = "en"
    override var name: String="爱壹帆"
    override var hostUrl = "https://www.iyf.tv"
    override var isRateLimited=true
    private val headers = mapOf("referer" to "https://www.iyf.tv", "user-agent" to USER_AGENT, "X-Requested-With" to "XMLHttpRequest")
    private val privateKeys= listOf("version001", "vers1on001", "vers1on00i", "bersion001", "vcrsion001", "versi0n001", "versio_001", "version0o1")
    private val expire=1679785424.89539
    private val sign = "8559cc6601ca996125d275a64e789f56e2dec45cb3ab3ae5c6fe7e9aa12a18bc_f7e37e485b541d9683a18b7f049bcd8c"
    private val token = "11e7880766664a628d5942c45ecc3660"
    private var uid = 590426
    private var publicTimeKey=System.currentTimeMillis()
    private var privateTimeKey=privateKeys[(publicTimeKey.mod(privateKeys.size))]
    private var publicKey = ""
    private var privateKey = ""
    private val MD5 = MessageDigest.getInstance("MD5")
    init {
        GlobalScope.launch { updateKeys() }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlayListVideo(
        @JsonProperty("key") val key: String,
        @JsonProperty("name") val name: String,
    )
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchResultData(
        @JsonProperty("title") val title: String,
        @JsonProperty("vipResource") val vipResource: String,
        @JsonProperty("atypeName") val type: String,
        @JsonProperty("directed") val directed: String,
        @JsonProperty("hot") val hot: Int,
        @JsonProperty("favoriteCount") val favoriteCount: Int,
        @JsonProperty("imgPath") val imgPath: String,
        @JsonProperty("lang") val lang: String,
        @JsonProperty("cid") val cid: String,
        @JsonProperty("lastName") val lastName: String,
        @JsonProperty("rating") val rating: String,
        @JsonProperty("starring") val starring: String,
        @JsonProperty("addTime") val addTime: String,
        @JsonProperty("contxt") val contxt: String,
    )
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FilterSearchResult(
        @JsonProperty("title") val title: String,
        @JsonProperty("vipResource") val vipResource: String,
        @JsonProperty("atypeName") val type: String,
        @JsonProperty("directed") val directed: String,
        @JsonProperty("hot") val hot: Int?,
        @JsonProperty("favoriteCount") val favoriteCount: Int,
        @JsonProperty("image") val image: String,
        @JsonProperty("lang") val lang: String,
        @JsonProperty("cidMapper") val cidMapper: String,
        @JsonProperty("lastName") val lastName: String,
        @JsonProperty("starring") val starring: String,
        @JsonProperty("addTime") val addTime: String,
        @JsonProperty("contxt") val contxt: String,
        @JsonProperty("videoClassID") val videoClassID: String,
        @JsonProperty("updateweekly") val updateweekly: String,
        @JsonProperty("key") val key: String,
        @JsonProperty("rating") val rating: String)
    private fun vv(o:String) = BigInteger(1, MD5.digest(o.toByteArray(StandardCharsets.UTF_8))).toString(16).padStart(32, '0')
    suspend fun updateKeys() {
        val o = "cinema=1".lowercase()
        publicTimeKey=System.currentTimeMillis()
        privateTimeKey=privateKeys[(publicTimeKey.mod(privateKeys.size))]
        val vv = vv("$publicTimeKey&$o&$privateTimeKey")
        val pConfig:JSONObject=app.get("https://m10.iyf.tv/v3/home/config?$o&vv=$vv&pub=$publicTimeKey",headers).json.unwrap().getJSONObject("pConfig")
        publicKey=pConfig.getString("publicKey")
        privateKey = pConfig.getJSONArray("privateKey").getString(0)
        println(publicKey)
    }

    private suspend fun callAPI(url: String, o: String): JSONObject {
        val vv = vv("$publicKey&${o.lowercase()}&$privateKey")
        val response = app.get("$url$o&vv=$vv&pub=$publicKey",headers)

        val data = response.json.getJSONObject("data")

        return data.getJSONArray("info").getJSONObject(0)
    }
//    if(data.getString("msg")=="访问过量"){
//        uid+=1
//        response=app.get("$url$o&vv=$vv&pub=$publicKey").json
//        data=response.getJSONObject("data")
//    }
    private val cid = hashMapOf(
    TvType.Movie to "0,1,3",
        TvType.TvSeries to "0,1,4",
        TvType.Reality to "0,1,5",
        TvType.Anime to "0,1,6",
        TvType.Documentary to "0,1,7")
    private val types = hashMapOf("动漫" to TvType.Anime,
    "电视剧" to TvType.TvSeries,
    "电影" to TvType.Movie,
    "综艺" to TvType.Reality,
    "jilupian" to TvType.Documentary
    )
    private suspend fun filterSearch(page: Int, latest:Boolean, type: TvType?): List<ShowResponse> {
        val orderBy = if(latest) 1 else 2
        val o = "cinema=1&page=$page&size=36&orderby=$orderBy&desc=1&cid=${cid[type]}&isserial=-1&isIndex=-1&isfree=-1"
        var info:List<FilterSearchResult>? = null
        var retries = 1
        while(info==null){
            try{
                val apiResults = callAPI("https://m10.iyf.tv/api/list/Search?",o).getJSONArray("result").toString()
                info = apiResults.parsed<List<FilterSearchResult>>()
            }catch (e:Exception){
                retries+=1
                println("try:$retries")
            }
        }
        return info.map {
            val status = if(it.lastName=="已完结") Status.Completed else Status.Ongoing
            val views = it.hot?.format()
            ShowResponse(
                it.title, provider = name, type, it.key, "${it.image}?w=216&height=309&format=jpg&mode=stretch",
                it.lastName, it.addTime, status, it.updateweekly, ""
            ).apply {
                genres =  it.cidMapper.split(',').toMutableList()
                this.views =views
                this.ratings = it.rating
            }
        }
    }
    private fun getStatus(status:String): Status {
        return when(status){
            "已完结"-> Status.Completed
            else -> Status.Ongoing
        }
    }
    private fun Int.format()=NumberFormat.getNumberInstance(Locale.US).format(this)
    private fun JSONObject.unwrap(): JSONObject =this.getJSONObject("data").getJSONArray("info").getJSONObject(0)
    override suspend fun search(query:String, page:Int, type: TvType): List<ShowResponse> {
        val tag = query.urlEncoded().lowercase()
        val vv = vv("${publicTimeKey}tags=$tag$privateTimeKey")
        val url = "https://rankv21.iyf.tv/v3/list/briefsearch?tags=$tag&orderby=4&page=$page&size=36&desc=1&isserial=-1"
        val data = app.post(url
            ,headers, data=mapOf("tag" to tag,"vv" to vv, "pub" to publicKey)).json.unwrap().getJSONArray("result").toString()
        return data.parsed<List<SearchResultData>>().map {
            ShowResponse(
                it.title, provider = name, types[it.type], it.contxt,
                "${it.imgPath}?format=jpg&mode=stretch", it.lastName, it.addTime, getStatus(it.lastName)
            ).apply {
                    genres.add(it.cid);ratings =it.rating
                    views =it.hot.format()
                    ratings =it.rating
                }
        }.filter { it.type==type }
    }
    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> = filterSearch(page,false,type)
    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> = filterSearch(page,true,type)
    override suspend fun loadDetails(show: ShowResponse): ShowResponse {
        var o = "cinema=1&device=1&player=CkPlayer&tech=HLS&country=HU&lang=cns&v=1&id=${show.url}"
        val info = callAPI("https://m10.iyf.tv/v3/video/detail?",o)
        o = "cinema=1&vid=${show.url}&lsk=1&taxis=0&cid=${info.get("cid")}&uid=$uid&expire=$expire&gid=4&sign=$sign&token=$token"
        val playlistInfo = callAPI("https://m10.iyf.tv/v3/video/languagesplaylist?",o).getJSONArray("playList").toString()
        val episodes = playlistInfo.parsed<List<PlayListVideo>>().map { episode->
            SEpisode(
            show.title,
            episode.name,
            episode.name.filter{ c: Char ->  c.isDigit()}.toIntOrNull() ?:  "0".toInt(),
            episode.key
        ) }
        return show.apply {
            setEpisodes(episodes)
            description =info.get("contxt").toString()
            updateSchedule=info.get("updateweekly").toString()
            provider = name
        }
    }

    override suspend fun loadSource(episode: SEpisode): Video? {
        val o = "cinema=1&id=${episode.url}&a=0&usersign=1&device=1&isMasterSupport=6&sharpness=1080&uid=$uid&expire=$expire&gid=4&sign=true&token=true"

        val vv = vv("$publicKey&${o.lowercase()}&$privateKey")
        val response = app.get("https://m10.iyf.tv/v3/video/play?$o&vv=$vv&pub=$publicKey",headers)
        println("https://m10.iyf.tv/v3/video/play?$o&vv=$vv&pub=$publicKey")
//        println(response.text)
        val info = callAPI("https://m10.iyf.tv/v3/video/play?",o).getJSONArray("clarity").toString()

        if (!info.contains("masterplaylist"))throw Exception("Master Playlist not found")
        val qualities = info.parsed<List<Clarity>>().sortedByDescending { it.bitrate }
        val bestQuality=qualities[0]
        val bitrate=(bestQuality.bitrate/1000).toString()
        val path = bestQuality.path.result.substringBeforeLast('&')
        return Video(episode.episodeName, episode.showName, path, "https://www.iyf.tv", true, quality = bitrate)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Clarity(
        @JsonProperty("bitrate") val bitrate: Int,
        @JsonProperty("path") val path: ClarityPath
    )
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ClarityPath(@JsonProperty("result") val result:String)
}
