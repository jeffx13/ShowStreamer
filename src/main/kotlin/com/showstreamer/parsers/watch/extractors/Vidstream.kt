package com.showstreamer.parsers.watch.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.showstreamer.app
import com.showstreamer.parsers.Video
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime.Companion.baseTable1
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime.Companion.githubHeaders
import com.showstreamer.utils.base64Decoded
import com.showstreamer.utils.parsed
import kotlinx.coroutines.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.properties.Delegates

object Vidstream {
    private const val jsonLink = "https://raw.githubusercontent.com/AnimeJeff/Overflow/main/syek"
    private var lastChecked = 0L
    private var cipherKey: VizCloudKey? = null
    private lateinit var operationMap:Map<Int,Operation>
    private suspend fun getKey(): VizCloudKey {
        cipherKey =
            if (cipherKey != null && (lastChecked - System.currentTimeMillis()) < 1000 * 60 * 30) cipherKey!!
            else {
                lastChecked = System.currentTimeMillis()
                app.get(jsonLink,githubHeaders).text.base64Decoded().base64Decoded().base64Decoded().parsed<VizCloudKey>().also { parseKeys(it) }
            }
        return cipherKey!!
    }
    data class VizCloudKey(
        @JsonProperty("cipherKey") val cipherKey: String,
        @JsonProperty("mainKey") val mainKey: String,
        @JsonProperty("encryptKey") val encryptKey: String,
        @JsonProperty("pre") val pre: List<String>,
        @JsonProperty("post") val post: List<String>,
        @JsonProperty("operations") val operations: Map<Int,String>,

        )
    private fun parseKeys(keys:VizCloudKey){
        operationMap=keys.operations.mapValues {
            val operationString = it.value.split(" ")
            getOperator(operationString)
        }
        println(keys)
    }
    suspend fun extract(cloudId:String?): Video? {
        cloudId ?: return null
        val serverUrl: String
        val referer: String
        if (cloudId.length < 7) {
            serverUrl = "https://mcloud.to"
            referer = "https://mcloud.to/e/$cloudId?autostart=true"
        } else {
            serverUrl = "https://vidstream.pro"
            referer = "https://vidstream.pro/embed/$cloudId?autostart=true"
        }
        val viz = getKey()
        val id = NineAnime.encrypt(NineAnime.cipher(viz.cipherKey, cloudId),viz.encryptKey)
        var encrypted = id
        encrypted = encrypt(encrypted,viz.pre)
        encrypted = dashify(encrypted)
        encrypted = encrypt(encrypted,viz.post)


        val url="$serverUrl/${viz.mainKey.trim()}/$encrypted"
        val response=app.get(url,mapOf("X-Requested-With" to "XMLHttpRequest","Referer" to referer)).text
            println(url)
//        println(response)
        if(!response.contains("m3u8"))renewKeys("Overflow").also { println("Outdated");return null }
        val src = checkSources(response)
        src ?: renewKeys("Overflow").also { println("404 not found");return null }
        return Video(url = src!!, referer = serverUrl, isM3u8 = true, quality = "")
    }
    private suspend fun checkSources(response:String): String? {
        sourceRegex.findAll(response).forEach {
            val url=it.value.replace("\\/","/")
            val resp = app.get(url)
            resp.body?.close()
            if(resp.code!=404)return url
        }
        return null
    }
    private val sourceRegex=Regex("""(?<="file":")https:[^:"]+m3u8(?=")""")
    private suspend fun encrypt(input: String, steps:List<String>): String {
        var output:String= input
        val viz= getKey()
        for(step in steps){
            when(step){
                "o"->{output=NineAnime.encrypt(output,viz.encryptKey).replace("/", "_");
                    println("o: "+NineAnime.encrypt(output,viz.encryptKey)+" and "+output)}
                "s"->{output=s(output)}
                "a"->{output=output.split("").reversed().joinToString("")}
            }

        }
        return output
    }
    suspend fun renewKeys(repository:String){
        lastChecked= 0L
        val client = HttpClient.newBuilder().build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/AnimeJeff/$repository/actions/workflows/workflow.yml/dispatches"))
            .POST(HttpRequest.BodyPublishers.ofString(" {\"ref\":\"main\"}"))
            .header("Authorization", "token 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").header("Accept","application/vnd.github.everest-preview+json")
            .build()
        withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        };
    }
    private fun dashify(input:String): String {
        val mapped = input.mapIndexedNotNull { i,c->
            operationMap[i % operationMap.size]!!.calculate(c.code)
        }.joinToString("-")
        return mapped
    }
    suspend fun dashifyV2(input:String): String {
        val key=getKey()
        val mapped = input.mapIndexedNotNull { i, c ->
            val operation=key.operations[i % key.operations.size]!!.split(" ")
            val operand = operation[1].toInt()
            when(operation.first()){
                "*"  -> c.code * operand
                "+"  -> c.code + operand
                "-"  -> c.code - operand
                "<<" -> c.code shl operand
                "^"  -> c.code xor operand
                else -> null
            }
        }.joinToString("-")
        return mapped
    }
    private fun getOperator(operation:List<String>):Operation{
        val operand = operation[1].toInt()
        return when(operation[0]){
            "+" -> Add(operand)
            "-" -> Subtract(operand)
            "*" -> Multiply(operand)
            ">>" -> Shr(operand)
            "<<" -> Shl(operand)
            "^" -> Xor(operand)
            else -> throw Exception("unidentified operator")
        }
    }
    private fun s(g:String): String {
        return g.replace("[a-zA-Z]".toRegex()) {
            val a = if (it.value.first().code <= 90) 90 else 122
            val b = it.value.first().code + 13
            (if (a >= b) b else b - 26).toChar().toString()
        }
    }
}
abstract class Operation {
    open val b:Int = 1

    abstract fun calculate(a: Int): Int
}
class Add(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a + b
    }
}
class Subtract(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a - b
    }
}
class Multiply(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a * b
    }
}
class Shr(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a shr b
    }
}
class Shl(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a shl b
    }
}
class Xor(override val b:Int):Operation(){
    override fun calculate(a: Int): Int {
        return  a xor b
    }
}








