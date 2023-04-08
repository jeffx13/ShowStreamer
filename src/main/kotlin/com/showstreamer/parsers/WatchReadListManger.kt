package com.showstreamer.parsers

import com.showstreamer.ShowStreamer
import com.showstreamer.mapper
import com.showstreamer.parsers.read.ReadResponse
import com.showstreamer.parsers.watch.ShowResponse
import com.fasterxml.jackson.core.type.TypeReference
import java.io.File

object WatchReadListManger {
    private val fileDirectory = File(javaClass.protectionDomain.codeSource.location.toURI()).absolutePath.substringBeforeLast('\\')
    private val watchListFile= File("$fileDirectory\\watch-list.json")
    private val readListFile= File("$fileDirectory\\read-list.json")
    val watchlist:MutableList<ShowResponse>
    val readList:MutableList<ReadResponse>
    init {
        if(!watchListFile.exists()){
            watchListFile.createNewFile()
            watchListFile.writeText("[]")
        }
        if(!readListFile.exists()){
            readListFile.createNewFile()
            readListFile.writeText("[]")
        }
        watchlist= mapper.readValue(watchListFile,object : TypeReference<MutableList<ShowResponse>>() {})
        readList= mapper.readValue(readListFile,object : TypeReference<MutableList<ReadResponse>>() {})
    }
    fun addToWatch(show: ShowResponse){
        watchlist.add(ShowResponse(show.title, show.provider, show.type, show.url, show.posterUrl, listType = ListType.values().random()))
        ShowStreamer.mainController.watchListPaneController.addShow(show)
        updateWatchListFile()
    }
    fun removeFromWatch(show: ShowResponse){
        watchlist.removeIf{it.url==show.url}
        updateWatchListFile()
    }
    fun isInWatch(url: String)=watchlist.find { it.url == url }!=null
    private fun updateWatchListFile()= mapper.writeValue(watchListFile,watchlist)

    fun addToRead(read: ReadResponse){
        readList.add(ReadResponse(read.title, read.provider, read.type, read.url, read.posterUrl))
        updateReadListFile()
    }
    fun removeFromRead(read: ReadResponse){
        readList.removeIf{it.url==read.url}
        updateReadListFile()
    }
    fun isInRead(url: String)=readList.find { it.url == url }!=null
    private fun updateReadListFile()= mapper.writeValue(readListFile,readList)
}