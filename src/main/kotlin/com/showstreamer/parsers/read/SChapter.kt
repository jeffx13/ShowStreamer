package com.showstreamer.parsers.read

import javafx.beans.property.SimpleBooleanProperty

data class SChapter(
    val title: String,
    val chapterName:String,
    val number:Float,
    val url:String,
    var resources:List<String> = listOf(),
    var isSelected: SimpleBooleanProperty = SimpleBooleanProperty(false),
    var referer: String?=null
)