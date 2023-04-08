package com.showstreamer.parsers.read.mangaproviders

import com.showstreamer.app
import com.showstreamer.parsers.read.ReadResponse
import com.showstreamer.parsers.ReadType
import com.showstreamer.parsers.read.SChapter
import com.showstreamer.parsers.Status
import com.showstreamer.parsers.read.ReadProvider
import com.showstreamer.utils.*
import kotlinx.coroutines.*
import org.jsoup.nodes.Document

class Manganato : ReadProvider() {
    override val name: String = "Manganato"
    override val hostUrl: String = "https://manganato.com/"
    override val language: String ="en"
    private fun parseGenreSearch(document:Document): List<ReadResponse> {
        return document.select(".content-genres-item").map {
            val a = it.selectFirst(".genres-item-img")!!
            val latestChapter=""//it.selectText(".genres-item-chap.text-nowrap.a-h")
            ReadResponse(a.title,name, ReadType.Manga,a.href,a.selectFirst("img")!!.src,latestChapter)
        }
    }
    override suspend fun search(query: String, page: Int, readType: ReadType): List<ReadResponse> {
        return app.get("https://manganato.com/search/story/${query.urlEncoded()}?page=$page").document.select(".search-story-item").map {
            val anchor = it.selectFirst("a.item-img")!!
            val latestChapter=it.selectFirst(".item-chapter.a-h.text-nowrap")!!.text().substringBefore(":")
            val author = it.selectFirst(".text-nowrap.item-author")!!.text()
            val info = it.select(".text-nowrap.item-time")
            ReadResponse(
                anchor.title,
                name,
                ReadType.Manga,
                anchor.href,
                anchor.selectFirst("img")!!.src,
                latestChapter,
                info.last()?.text()?.substringAfter(": "),
                "",
                author,
                "",
                "",
                Status.Unknown,
                info.first()?.text()?.substringAfter(": ")
            )
        }
    }

    override suspend fun popular(page: Int, type: ReadType): List<ReadResponse> {
        return emptyList()
    }

    override suspend fun latest(page: Int, type: ReadType): List<ReadResponse> {
        return parseGenreSearch(app.get("https://manganato.com/genre-all/$page").document)
    }

    override suspend fun loadDetails(readResponse: ReadResponse): ReadResponse {
        val document=app.get(readResponse.url).document
        readResponse.alternative=document.selectFirst(".info-alternative")?.parent()?.nextElementSibling()?.text()
            ?.substringBefore("(English)")?.substringAfterLast(",") ?: ""
        readResponse.author=document.selectFirst(".info-author")?.parent()?.nextElementSibling()?.text() ?: ""
        readResponse.status=when(document.selectFirst(".info-status")?.parent()?.nextElementSibling()?.text()){
            "Ongoing"-> Status.Ongoing
            else-> Status.Completed
        }
        document.selectFirst(".info-author")?.parent()?.nextElementSibling()?.select("a")?.map { it.text() }?.toList()
            ?.let { readResponse.genres.addAll(it) }

        val stre_label=document.select(".stre-value")
        readResponse.updateSchedule=stre_label.first()!!.text()
        readResponse.views=stre_label[1].text()
        readResponse.ratings=document.selectText("#rate_row_cmd > em > em:nth-child(2) > em > em:nth-child(1)")
        readResponse.chapters=document.select("ul.row-content-chapter li.a-h").map {
            val anchor=it.selectFirst("a")
            val chapterName=anchor!!.text()
            val number=chapterName.substringAfter("ter ")//.toFloat()
            var chapNum:Float
            try {
                chapNum = "(\\d+(?:\\.\\d+|-\\d+)?)".toRegex().find(number)!!.value.replace("-", ".").toFloat()
            }catch (e:Exception){println(number);chapNum=0.0f}

            SChapter(readResponse.title,chapterName,chapNum,anchor.href) }.sortedBy { it.number }.toMutableList()
        readResponse.description=document.selectText(".panel-story-info-description")
        return readResponse
    }

    override suspend fun loadSource(chapter: SChapter): SChapter {

        GlobalScope.launch {
            var pages: List<String> = app.get(chapter.url).document.select(".container-chapter-reader img").map {
//                async {
//                    val image = app.get(it.src, referer = "https://readmanganato.com/").body!!.byteStream()
////                    ImageView(Image(image))
//                    Image(image)
//                }
                it.src
            }
            chapter.resources=pages
            chapter.referer="https://readmanganato.com/"
        }.join()

        return chapter
    }


}