package com.showstreamer.parsers

abstract class BaseProvider<Response,Type> {

    abstract val name: String

    open val saveName: String = ""

    abstract val hostUrl: String

    open val isNSFW = false

    open val language = "English"

    abstract suspend fun search(query:String, page:Int, type: Type): List<Response>
    abstract suspend fun popular(page: Int, type: Type): List<Response>
    abstract suspend fun latest(page: Int, type: Type): List<Response>
    abstract suspend fun loadDetails(show: Response): Response

    protected fun fixUrl(url:String) = hostUrl+url

//    abstract suspend fun search(query: String): List<ShowResponse>


//    open suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse? {
//        checkIfVariablesAreEmpty()
//        return loadData("${saveName}_$mediaId")
//    }
//
//    open fun saveShowResponse(mediaId: Int, response: ShowResponse?, selected: Boolean = false) {
//        if (response != null) {
//            checkIfVariablesAreEmpty()
//            setUserText("${if (selected) "Selected" else "Found"} : ${response.name}")
//            saveData("${saveName}_$mediaId", response)
//        }
//    }

}