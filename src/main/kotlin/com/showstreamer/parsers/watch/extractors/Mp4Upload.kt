import com.showstreamer.app

object Mp4Upload{
    suspend fun extract(url:String){
        val linkRegex="""(?<='\|\|)[^']+""".toRegex()
        val packed = linkRegex.find(app.get(url).text)?.value?.split("|")?.reversed() ?: return
        val link = "https://www2.mp4upload.com:${packed[2]}/d/${packed[3]}/video.mp4"
        println(link)
        println(app.get(link, referer = "https://www.mp4upload.com/"))
        throw NotImplementedError("need insecure")
    }
}