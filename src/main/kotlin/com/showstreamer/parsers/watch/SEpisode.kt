package com.showstreamer.parsers.watch

import javafx.beans.property.SimpleBooleanProperty

data class SEpisode(
    var showName: String,
    var episodeName: String,
    var number: Int,
    var url: String,
    var isSelected: SimpleBooleanProperty = SimpleBooleanProperty(false)
)
data class Server(
    var serverName:String,
    val url:String,
    val referer:String,
    val isM3u8:Boolean
)
data class SkipData(
    val introBegin:Int,
    val introEnd:Int,
    val outroBegin:Int,
    val outroEnd:Int,
)