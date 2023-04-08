package com.showstreamer.parsers.read

import com.showstreamer.parsers.BaseProvider
import com.showstreamer.parsers.ReadType

abstract class ReadProvider: BaseProvider<ReadResponse, ReadType>() {
    abstract suspend fun loadSource(chapter: SChapter): SChapter
}