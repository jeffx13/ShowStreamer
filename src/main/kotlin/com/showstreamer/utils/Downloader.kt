package com.showstreamer.utils

import com.showstreamer.controllers.MainController
import com.showstreamer.parsers.Video
import java.io.File

object Downloader {
    private val processes = mutableListOf<Process>()
    private fun downloadM3u8(folder:String,video: Video):Process?{
        if(video.shouldStopDownloading.value)return null
        video.name =video.name.replace(":",".")

        val videoPath = File("${folder}/${video.name}.mp4")

        val headers= "authority:\"AUTHORITY\"|origin:\"https://REFERER\"|referer:\"https://REFERER/\"|user-agent:\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36\"sec-ch-ua:\"Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Google Chrome\";v=\"102\"".replace("REFERER", video.referer
            ?: video.url.split("https://")[1].split("/")[0])
        val command = mutableListOf(MainController.nilaodaPath, video.url,"--workDir",folder,"--saveName",video.name,"--enableDelAfterDone","--disableDateInfo","--noProxy","--headers",headers)

        val builder = ProcessBuilder(command)
        val p = builder.start().also { processes.add(it) }
        return p
    }
    fun download(folder:String,video: Video): Process? {
        if (video.isM3u8) {
            return downloadM3u8(folder,video)
        }
        return null
    }
    fun finalize() {
        processes.forEach { it.destroyForcibly() }
    }
}