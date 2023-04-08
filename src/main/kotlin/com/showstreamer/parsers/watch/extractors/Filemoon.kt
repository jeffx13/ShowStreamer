package com.showstreamer.parsers.watch.extractors

import com.showstreamer.app
import com.showstreamer.utils.JsUnpacker

object Filemoon {
    private val linkRegex="""(?<=file:")https[^"]+""".toRegex()
    suspend fun extract(url: String): String? {
        val unpacked = JsUnpacker(app.get(url).text).unpack() ?: return null
        return linkRegex.find(unpacked)?.value
    }
}