package com.showstreamer.parsers.watch.providers.animeproviders

import com.showstreamer.*
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.setEpisodes
import com.showstreamer.utils.*
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Gogoanime: ShowProvider() {
    override var language = "en"
    override var name: String="Gogoanime"
    override var hostUrl = "https://gogoanime.lu"

    val iv = "3134003223491201"
     val secretKey = "37911490979715163134003223491201"
     val secretDecryptKey = "54674138327930866480207815084989"
    private val headers = mapOf("referer" to "https://goload.pro","X-Requested-With" to "XMLHttpRequest")
    override suspend fun search(query: String, page: Int, type: TvType): List<ShowResponse>{
        return app.get("$hostUrl/search.html?keyword=$query&page=$page").document.select(""".last_episodes li""").map {
            val name = it.selectFirst(".name")!!.text()
            val url = "$hostUrl/" + it.selectHref("a")
            val show = ShowResponse(
                name, this.name, TvType.Anime,
                url, it.selectFirst("img")!!.src, year=it.selectFirst(".released")?.text()?.split(":")?.getOrNull(1)?.trim()
            )
            show
        }
    }
    override suspend fun popular(page: Int, type: TvType): List<ShowResponse> {

        return app.get("https://ajax.gogo-load.com/ajax/page-recent-release-ongoing.html?page=$page",headers).document
            .select(".added_series_body.popular li").map {
                val anchor = it.selectFirst("a")!!
                val url = "$hostUrl${anchor.href}"
                val posterUrl = anchor.selectFirst(".thumbnail-popular")!!.attr("style").substringAfter("url('").substringBefore("');")
                val genres = it.select("p.genres a").map{ a -> a.text() }
                val episodeTxt = it.select("p").last()?.text()?.substringAfter("Episode ")
                ShowResponse(
                    anchor.title, provider = name, TvType.Anime,
                    url, posterUrl, episodeTxt=episodeTxt
                ).apply { this.genres.addAll(genres) }
            }
    }
    override suspend fun latest(page: Int, type: TvType): List<ShowResponse> {
        return app.get("https://ajax.gogo-load.com/ajax/page-recent-release.html?page=$page&type=1").document.select(""".last_episodes li""").map {
            val name = it.selectFirst(".name")!!.text()
            val url = "$hostUrl/category" + it.selectHref("a").substringBefore("-episode")
            val show = ShowResponse(
                name, provider = this.name, TvType.Anime,
                url, it.selectFirst("img")!!.src, it.selectFirst(".episode")!!.text().substringAfter("Episode ")
            )
            show
        }

    }
    override suspend fun loadDetails(show: ShowResponse): ShowResponse {

        val document = app.get(show.url,headers).document
        val episodes = document.select("ul#episode_page li a").last()!!.attr("ep_end").toInt()
        document.select(" div.anime_info_body_bg > p span").map {
            when(it.text()){
                "Plot Summary:" -> show.description =it.parent()!!.text().substringAfter(":")
                "Status:" -> show.status =if(it.parent()!!.text().substringAfter(":")=="Ongoing") Status.Ongoing else Status.Completed
                "Released:" -> show.year =it.parent()!!.text().substringAfter(":")
                "Genre:" -> show.genres = it.parent()!!.text().substringAfter(":").split(", ").toMutableList()
                else -> {}
            }
        }
        show.setEpisodes((1..episodes).map { SEpisode(
            show.title,
            it.toString(),
            it,
            "$hostUrl/${show.url.substringAfterLast('/')}-episode-$it"
        ) })
        return show
    }
    override suspend fun loadSource(episode: SEpisode): Video? {
        val iframeSrc = "https:"+app.get(episode.url).document.selectFirst("iframe")?.src
        var gogocdnUrl:String?=null
        app.get(iframeSrc).document.select(".list-server-items > .linkserver").forEach {
            val serverUrl = it.attr("data-video")
            if(serverUrl.contains("goload"))gogocdnUrl=serverUrl
        }
        val id = Regex("id=([^&]+)").find(gogocdnUrl!!)!!.value.removePrefix("id=")
        val document=app.get(gogocdnUrl!!,headers).document
        val encryptedId = cryptoHandler(id, iv, secretKey)
        val encryptRequestData = if (true) {
            val dataEncrypted =
                document.select("script[data-name='episode']").attr("data-value")
            val headers = cryptoHandler(dataEncrypted, iv, secretKey, false)
            "id=$encryptedId&alias=$id&" + headers.substringAfter("&")
        } else {
            "id=$encryptedId&alias=$id"
        }
        val jsonResponse = app.get("https://goload.pro/encrypt-ajax.php?$encryptRequestData",
            mapOf("X-Requested-With" to "XMLHttpRequest"))
        val dataEncrypted = jsonResponse.text.substringAfter("{\"data\":\"").substringBefore("\"}")
        val dataDecrypted = cryptoHandler(dataEncrypted, iv, secretDecryptKey, false)
        val source = JSONObject(dataDecrypted).getJSONArray("source").getJSONObject(0)["file"].toString()
        return Video(episode.episodeName, episode.showName, source, null, true, quality = null)
    }

    override var isRateLimited = false

    fun cryptoHandler(
        string: String,
        iv: String,
        secretKeyString: String,
        encrypt: Boolean = true
    ): String {
        val ivParameterSpec = IvParameterSpec(iv.toByteArray())
        val secretKey = SecretKeySpec(secretKeyString.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        return if (!encrypt) {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            String(cipher.doFinal(Base64.decodeBase64(string)))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            Base64.encodeBase64String(cipher.doFinal(string.toByteArray()))
        }
    }
}