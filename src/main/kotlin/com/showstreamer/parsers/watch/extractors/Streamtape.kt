package com.showstreamer.parsers.watch.extractors

import com.showstreamer.app
import java.io.File

object Streamtape {
    private val linkRegex = Regex("""'robotlink'\)\.innerHTML = '(.+?)'\+ \('(.+?)'\)""")
    suspend fun extract(url:String): String {
        val reg = linkRegex.find(app.get(url).text) ?: return ""
        val extractedUrl = "https:${reg.groups[1]!!.value + reg.groups[2]!!.value.substring(3)}"
        println(extractedUrl)
        return extractedUrl
//        File("D:\\TV\\test.mp4").writeBytes(app.get(extractedUrl,referer=url).body!!.bytes())

    }
}