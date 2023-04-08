package com.showstreamer.parsers.watch.extractors

import com.showstreamer.app
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.utils.base64Decoded
import com.showstreamer.utils.json
import com.showstreamer.utils.parsed

object Player_aaaa {
    val player_aaaa_regex=Regex("(?<=var player_aaaa=).*?(?=</script>)")
    val configRegex = Regex("""(?<=var config = )\{[^}]+}""")
    val decodedUrlRegex = Regex("""http.*?e=[\d]+""")
    suspend fun extract(url:String): String {
        val player_aaaa = player_aaaa_regex.find(app.get(url).text)!!.value.json
        val url = player_aaaa.getString("url")
        val decrypted=when(player_aaaa.getInt("encrypt")){
            1-> url
            2-> url.base64Decoded()
            else -> url
        }
        return decrypted
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Config(
        @JsonProperty("url") val url:String,
        @JsonProperty("vkey") val vkey:String,
        @JsonProperty("token") val token:String,
        @JsonProperty("sign") val sign:String="smdyycc",
        )
    suspend fun getConfig(url:String){
        val config = configRegex.find(app.get(url).text)!!.value.parsed<Config>()
        val url = app.post("https://player.6080kan.cc/player/xinapi.php",data= mapOf("url" to config.url,"vkey" to config.vkey,"token" to config.token,"sign" to config.sign))
            .json.getString("url")
        decodedUrlRegex.find(url.base64Decoded())?.value
    }
    suspend fun test(){
        val encrypted=Player_aaaa.extract("https://www.smdyy.cc/play/116-2-70.html")
        val config = Player_aaaa.getConfig("https://player.6080kan.cc/player/play.php?url=$encrypted")
    }
}