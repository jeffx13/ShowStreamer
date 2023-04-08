package com.showstreamer.parsers.read

import com.showstreamer.ShowStreamer
import com.showstreamer.controllers.MainController
import com.showstreamer.controllers.reader.ReaderHomeController
import com.showstreamer.parsers.*
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList

object ReadAPI:MainAPI<ReadResponse, ReadProvider, ReadType>(){
    private var currentSelection: ReadResponse?=null
    private lateinit var home:ReaderHomeController
    val chapterList: ObservableList<SChapter> = FXCollections.observableList(arrayListOf<SChapter>())
    private val detailCache = HashMap<ReadResponse, ReadResponse>()
    private val chapterCache = HashMap<Int, SChapter>()
    override fun init() {
        Platform.runLater {
            val home= MainController.readHomeController
            currentType.bind(home.typeComboBox.valueProperty())
            currentProvider.bind(home.providerComboBox.valueProperty())
            showSearchQuery.bind(home.titleTextField.textProperty())
            isLoadMoreDisabled.bindBidirectional(home.loadMoreButton.disableProperty())
        }
//        progressBar=ShowStreamer.showInfoController.linkProgressBar
    }

    override suspend fun loadPopular(): List<ReadResponse> {
        return currentProvider.value.popular(currentShowPage,currentType.value)
    }

    override suspend fun loadLatest(): List<ReadResponse> {
        return currentProvider.value.latest(currentShowPage,currentType.value)
    }

    override suspend fun loadSearch(): List<ReadResponse> {
        return currentProvider.value.search(showSearchQuery.value,currentShowPage,currentType.value)
    }

    override fun updateHistory(searchType: SearchType) {
        if(searchType== SearchType.WATCHLIST){currentShowPage=1;home.loadMoreButton.isDisable=true}
        showSearchHistory= SearchHistory(searchType,null,currentShowPage)
            .apply { if(searchType== SearchType.SEARCH)text= showSearchQuery.value;readType=currentType.value }
    }
    suspend fun loadDetails(readResponse: ReadResponse, callback: (ReadResponse) -> Unit){
        chapterCache.clear()
        //println(readResponse.provider)
        val provider = ReaderHomeController.getProviderByName(readResponse.provider)
        currentSelection = detailCache.getOrElse(readResponse) {
            provider.loadDetails(readResponse).also { detailCache[readResponse] = it }
        }
        callback(currentSelection!!)
    }

    suspend fun loadChapter(chapter:Int,callback: (SChapter) -> Unit){
        if(chapter >= chapterList.size || chapter < 0)return
        callback(chapterCache.getOrElse(chapter){
            currentProvider.value.loadSource(chapterList[chapter]).also { chapterCache[chapter] = it }
        })
    }
}