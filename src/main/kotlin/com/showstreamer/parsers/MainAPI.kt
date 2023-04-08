package com.showstreamer.parsers

import com.showstreamer.parsers.read.*
import com.showstreamer.parsers.watch.ShowResponse
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.*

abstract class MainAPI<Response,Provider,Type>{
    var showSearchHistory: SearchHistory?=null

    protected var currentProvider = SimpleObjectProperty<Provider>()
    protected val showSearchQuery = SimpleStringProperty("")
    val scope = CoroutineScope(CoroutineName("Search Results Scope"))
    protected var loadResultsJob: Job? = null
    protected var loadDetailsJob: Job? = null
    protected var currentShowPage:Int = 1
    protected val currentType = SimpleObjectProperty<Type>()
    protected val isLoadMoreDisabled = SimpleBooleanProperty(true)
    var currentShow: ShowResponse?=null
    val currentRead: ReadResponse?=null
    abstract fun init()

    fun loadMore(searchType: SearchType?, callback: (List<Response>) -> Unit){
        scope.launch {
            loadResultsJob?.join()
            loadResultsJob = launch {
                val type = searchType ?: showSearchHistory?.type ?: SearchType.LATEST.also { println("Default latest ") }
                currentShowPage+=1
                val results:List<Response> = when(type){
                    SearchType.POPULAR -> loadPopular()
                    SearchType.LATEST  -> loadLatest()
                    SearchType.SEARCH  -> loadSearch()
                    SearchType.WATCHLIST -> emptyList()
                    else -> emptyList() }
                callback(results)
                updateHistory(type!!)
            }
        }
    }
    abstract suspend fun loadPopular(): List<Response>
    abstract suspend fun loadLatest(): List<Response>
    abstract suspend fun loadSearch(): List<Response>
    private fun checkSearchHistory(searchType: SearchType): Boolean{
        return if(searchType== SearchType.SEARCH){
            (showSearchHistory?.type == searchType) && (showSearchHistory?.text == showSearchQuery.value) && (showSearchHistory?.tvType == currentType.value)
        }else{
            (showSearchHistory?.type == searchType) && (showSearchHistory?.tvType == currentType.value)
        }
    }

    fun loadResults(searchType: SearchType, callback:(List<Response>) -> Unit){
//        println(searchType)
        if(checkSearchHistory(searchType)){
            if(isLoadMoreDisabled.value){callback(emptyList());return}
            if(loadResultsJob?.isActive==false)loadMore(searchType){callback(it)}
        }else{
//            println(searchType)
            scope.launch {
                loadResultsJob?.join()
                currentShowPage=1
                isLoadMoreDisabled.value=false
                loadResultsJob= launch{
                    val results: List<Response> = when(searchType){
                        SearchType.LATEST-> loadLatest()
                        SearchType.POPULAR-> loadPopular()
                        else -> loadSearch()
                    }
//                    println(results)
                    updateHistory(searchType)
                    callback(results)
                }
            }
        }
    }

    abstract fun updateHistory(searchType: SearchType)

    protected fun finalize() {
        scope.cancel()
    }
}



