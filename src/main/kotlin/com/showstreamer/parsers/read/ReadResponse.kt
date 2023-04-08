package com.showstreamer.parsers.read

import com.showstreamer.parsers.ReadType
import com.showstreamer.parsers.Status

data class ReadResponse(
    var title: String,
    var provider: String,
    var type: ReadType? = null,
    var url: String,
    var posterUrl: String? = null,
    var episodeTxt: String? = null,
    var views: String? = "",
    var alternative: String = "",
    var author: String = "",
    var ratings: String? = "",
    var year: String? = null,
    var status: Status = Status.Completed,
    var updateSchedule: String? = "",
    var description: String? = "",
    var totalChapters: Int = 0,
    var genres: MutableList<String> = mutableListOf(),
    var chapters: MutableList<SChapter> = mutableListOf()
)