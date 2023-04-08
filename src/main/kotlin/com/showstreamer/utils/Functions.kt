package com.showstreamer.utils

import com.showstreamer.mapper
import com.fasterxml.jackson.core.type.TypeReference
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64


fun Element.selectText(css: String): String = selectFirst(css)!!.text()
fun Element.selectHref(css: String): String = selectFirst(css)!!.href
fun Element.selectInt(css: String) = selectFirst(css)!!.text().toIntOrNull()

val base64Encoder: Base64.Encoder = Base64.getEncoder()
val base64Decoder: Base64.Decoder = Base64.getDecoder()
fun String.base64Decoded():String = base64Decoder.decode(this).decodeToString()
fun String.base64Encoded(): String = base64Encoder.encodeToString(this.toByteArray())
fun ByteArray.base64Encoded(): String = base64Encoder.encodeToString(this)

inline fun <reified T : Any> String.parsed(): T {
    return mapper.readValue(this, object : TypeReference<T>() {})
}
val Element.href:String
    get() = this.attr("href")
val Element.src:String
    get() = this.attr("src")
val Element.title:String
    get() = this.attr("title")

val String.json:JSONObject
    get()= JSONObject(this)
val String.document: Document
    get()= Jsoup.parse(this)
fun String.substringBetween(first:String,second:String)=this.substringAfter(first).substringBefore(second)


fun String.urlEncoded()=URLEncoder.encode(this, "UTF-8")
fun String.urlDecoded()= URLDecoder.decode(this, "UTF-8")


val unixTimeMillis: Long
    get() = System.currentTimeMillis()
val unixTime: Long
    get() = System.currentTimeMillis() / 1000L