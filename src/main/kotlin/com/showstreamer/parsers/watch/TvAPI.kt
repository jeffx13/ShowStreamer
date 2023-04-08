package com.showstreamer.parsers.watch

import com.showstreamer.controllers.MainController
import com.showstreamer.controllers.show.SearchController
import com.showstreamer.parsers.*
import javafx.application.Platform
import javafx.scene.control.ProgressBar
import kotlinx.coroutines.*

object TvAPI: MainAPI<ShowResponse, ShowProvider, TvType>() {
    private var loadLinksJob: Job? = null
    private lateinit var progressBar:ProgressBar
    private lateinit var home:SearchController

    override fun init() {
        Platform.runLater {
            home = MainController.watchSearchController
            currentType.bind(home.typeComboBox.valueProperty())
            currentProvider.bind(home.providerComboBox.valueProperty())
            showSearchQuery.bind(home.titleTextField.textProperty())
            isLoadMoreDisabled.bindBidirectional(home.loadMoreButton.disableProperty())
            progressBar =MainController.watchInfoController.linkProgressBar
        }
    }

    override suspend fun loadPopular(): List<ShowResponse> {
        return currentProvider.value.popular(currentShowPage, currentType.value)
    }
    override suspend fun loadLatest(): List<ShowResponse> {
        return currentProvider.value.latest(currentShowPage, currentType.value)
    }
    override suspend fun loadSearch(): List<ShowResponse> {
        var query = showSearchQuery.value
        if (query.isEmpty())query="完美世界"
        return currentProvider.value.search(query, currentShowPage, currentType.value)
    }

    suspend fun loadSources(episodes:List<SEpisode>, provider: ShowProvider, callback: (MutableList<Video>) -> Unit) {
        val videos= mutableListOf<Video>()
        scope.launch {
            loadLinksJob?.cancelAndJoin()
            loadLinksJob = launch {
                if(provider.isRateLimited){
                    episodes.forEach {episode->
                        val src = provider.loadSource(episode)?.apply { name =episode.episodeName }
                        if(src!=null)videos.add(src) else println("${episode.episodeName} skipped")
                        progressBar.progress+=1.00/episodes.size
                        delay(2000)
                    }
                }else{
                    episodes.map{episode->
                        async (Dispatchers.IO){
                            try{
                                val src = provider.loadSource(episode)?.apply { name =episode.episodeName }
                                if(src!=null)videos.add(src) else println("${episode.episodeName} skipped")
                                progressBar.progress+=1.00/episodes.size
                            }catch (e:Exception){
                                e.printStackTrace()
                            }

                        }
                    }.awaitAll()
                }
                progressBar.progress=1.0
                if(videos.isNotEmpty())callback(videos)
            }
        }
    }

    override fun updateHistory(searchType: SearchType) {
        if(searchType== SearchType.WATCHLIST){currentShowPage=1;home.loadMoreButton.isDisable=true}
        showSearchHistory= SearchHistory(searchType,currentType.value,currentShowPage)
            .apply { if(searchType== SearchType.SEARCH)text= showSearchQuery.value }
    }


}