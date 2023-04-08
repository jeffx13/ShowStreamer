package com.showstreamer.controllers.reader

import com.showstreamer.*
import com.showstreamer.controllers.*
import com.showstreamer.controllers.show.ResponsePane
import com.showstreamer.parsers.*
import com.showstreamer.parsers.read.*
import com.showstreamer.parsers.read.ReadProvider
import com.showstreamer.parsers.read.mangaproviders.Manganato
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Rectangle2D
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.effect.ColorAdjust
import javafx.scene.effect.GaussianBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Callback
import kotlinx.coroutines.*
import org.controlsfx.control.GridCell
import org.controlsfx.control.GridView
import org.controlsfx.control.MasterDetailPane
import java.net.URL
import java.util.*

class ReaderHomeController:Initializable {

    lateinit var readResponseGridView: GridView<ResponsePane>
    lateinit var infoVbox: VBox
    lateinit var searchVbox: VBox
    lateinit var mainAnchorPane: AnchorPane
    private var loadChapterJob: Job?=null
    lateinit var latestButton: Button
    lateinit var popularButton: Button
    lateinit var infoBackgroundAnchorPane: AnchorPane
    lateinit var infoStackPane: StackPane
    lateinit var startReadingBtn: Button
    lateinit var descriptionTextArea: TextArea
    lateinit var genresFlowPane: FlowPane
    lateinit var infoBackgroundImageView: ImageView
    lateinit var infoTitleLabel1: Label
    lateinit var infoTitleLabel2: Label

    lateinit var infoAuthorLabel: Label
    lateinit var chaptersListView: ListView<SChapter>
    lateinit var infoImageView: ImageView
    lateinit var loadMoreButton: Button
    lateinit var searchButton: Button
    lateinit var titleTextField: TextField
    lateinit var providerComboBox: ComboBox<ReadProvider>
    lateinit var typeComboBox: ComboBox<ReadType>
    protected var currentPage = 1
    var searchHistory: SearchHistory? = null
    var cache = mutableListOf<ReadResponse>()
    var chapterCache = hashMapOf<String, SChapter>()
    lateinit var currentProvider: ReadProvider
    private var currentSelection: ReadResponse? = null
    private var loadResultsJob:Job?=null
    private val readerCoroutineScope = CoroutineScope(CoroutineName("Reader Coroutine Scope"))
    private val readResponses= FXCollections.observableList(mutableListOf<ResponsePane>())
    companion object{
        private val providers = FXCollections.observableList(arrayListOf<ReadProvider>(Manganato()))
        private lateinit var providerMap:HashMap<String, ReadProvider>
        fun getProviderByName(providerName:String): ReadProvider {
            if(!this::providerMap.isInitialized){
                providerMap= HashMap()
                providers.forEach{
                    providerMap[it.name] = it
                }
            }
            return providerMap[providerName] ?: Manganato()
        }
    }
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        typeComboBox.items.addAll(ReadType.Manga, ReadType.Novel)
        typeComboBox.value = ReadType.Manga
        providerComboBox.items=providers
        providerComboBox.value=providers.first()
        ReadAPI.init()
        providerComboBox.valueProperty().addListener { _, _, newValue ->
            loadMoreButton.isDisable = true
            currentProvider = newValue
            clearInfo()
        }


        infoBackgroundImageView.isManaged = false
        infoBackgroundImageView.fitWidthProperty().bind(infoStackPane.widthProperty())
        infoBackgroundImageView.fitHeightProperty().bind(infoBackgroundAnchorPane.heightProperty().multiply(0.9))
        infoBackgroundImageView.effect = ColorAdjust(0.0, 0.0, -0.6, 0.0).apply { input=GaussianBlur() }

        chaptersListView.cellFactory = Callback<ListView<SChapter>, ListCell<SChapter>> { ChapterListCell() }
        initGridView()
        initMasterDetailPane()
        setControlActions()
        Platform.runLater {
            chaptersListView.items=ReadAPI.chapterList
//            latestButton.fire()
            MainController.readViewController.readListView.items.addListener(ListChangeListener {
                it.next()
                ShowStreamer.mainController.readBtn.fire()
                MainController.readViewController.readListView.refresh()
            })
        }

    }
    private fun setControlActions(){
        loadMoreButton.setOnAction {
            ReadAPI.loadMore(null){
                addResponses(it)
            }
        }
        titleTextField.setOnKeyPressed { if (it.code == KeyCode.ENTER) searchButton.fire() }
        startReadingBtn.setOnAction {
            loadChapter(0)
        }
    }
    private fun initGridView(){
        return
        readResponseGridView.setCellFactory { ReadItemCell() }
        readResponseGridView.items=readResponses
        readResponseGridView.cellWidthProperty().set(MainController.imageWidth)
        readResponseGridView.cellHeightProperty().bind(readResponseGridView.cellWidthProperty().multiply(MainController.aspectRatio))

//        Platform.runLater {
//            val bar = readResponseGridView.lookup(".scroll-bar") as ScrollBar
//            bar.valueProperty().addListener { _, _, _ ->
//                if (bar.value == bar.max && !loadMoreButton.isDisable) {
//                    loadMoreButton.fire()
//                }
//            }
//            bar.visibleProperty().addListener { observable, _, _ ->
//                if (!observable.value && !loadMoreButton.isDisable) {
//                    readerCoroutineScope.launch {
//                        ReadAPI.loadMore(null) {
//                            addResponses(it)
//                            if (it.isNotEmpty()) {
//                                ReadAPI.loadMore(null) {
//                                    addResponses(it)
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }
    private fun initMasterDetailPane(){
        val masterDetail=MasterDetailPane(Side.RIGHT)
        mainAnchorPane.children.clear()
        
        masterDetail.detailNode=infoVbox
        masterDetail.masterNode=searchVbox//readResponseGridView
        mainAnchorPane.children.add(masterDetail)
        AnchorPane.setLeftAnchor(masterDetail,0.0)
        AnchorPane.setRightAnchor(masterDetail,0.0)
        AnchorPane.setTopAnchor(masterDetail,0.0)
        AnchorPane.setBottomAnchor(masterDetail,0.0)
        masterDetail.widthProperty().addListener { observable, oldValue, newValue ->
            masterDetail.resetDividerPosition()
        }
    }
    @FXML
    fun onLoadResults(event: ActionEvent){
        val searchType=when(event.source){
            searchButton -> SearchType.SEARCH
            latestButton -> SearchType.LATEST
            popularButton -> SearchType.POPULAR
            else -> return
        }

        ReadAPI.loadResults(searchType){
            setResponses(it)
        }
    }
    private fun setResponses(results: List<ReadResponse>){
//        infoImageView.image=null
        return
        if(results.isEmpty()){loadMoreButton.isDisable=true;return }
        Platform.runLater {
            readResponses.clear()
            addResponses(results)
        }
    }
    private fun addResponses(results: List<ReadResponse>){
        Platform.runLater{
            if(results.isEmpty()){loadMoreButton.isDisable=true;return@runLater }
            results.forEach { readResponses.add(it.forFlowPane())  }
            loadMoreButton.isDisable=false
        }
    }
    fun loadChapter(chapter: Int) {
        if (chapter >= chaptersListView.items.size || chapter < 0 || chapter == ReadController.currentChapter) return
        else ReadController.currentChapter = chapter
        readerCoroutineScope.launch {
            loadChapterJob?.cancelAndJoin()
            loadChapterJob = launch {
                ReadAPI.loadChapter(chapter) { src ->
                    MainController.readViewController.readListView.items.clear()
                    launch {
                        val pages=src.resources.mapIndexed { index, s ->
                            async {
                                val image= if(src.referer==null)Image(s,true)
                                else runBlocking { Image(app.get(s,referer=src.referer).body!!.byteStream()) }
                                Pair(index,image)
                            }
                        }.awaitAll()
                        Platform.runLater {
                            MainController.readViewController.readListView.items.addAll(pages.sortedBy { it.first })
                        }
                    }
                }
                ReadAPI.loadChapter(chapter + 1) {}
            }
        }
    }


    fun onChangeType(actionEvent: ActionEvent) {

    }
    private fun clearInfo(){
        descriptionTextArea.text=""
        infoTitleLabel1.text=""
        genresFlowPane.children.clear()
        chaptersListView.items.clear()
    }
    private fun loadDetails(readResponse: ReadResponse, image: Image){
        Platform.runLater {
            infoImageView.image=image
            infoTitleLabel1.text=readResponse.title
            infoBackgroundImageView.image=image
            infoBackgroundImageView.viewport= Rectangle2D(0.0,image.height*0.1,image.width,image.height*0.4)
            clearInfo()
            readerCoroutineScope.launch {
                ReadAPI.loadDetails(readResponse){
                    Platform.runLater{
                        infoAuthorLabel.text= it.author
                        infoTitleLabel2.text=it.alternative
                        descriptionTextArea.text=it.description
                        genresFlowPane.hgap=5.0
                        genresFlowPane.children.addAll(it.genres.map { genre->Label(genre).apply { textFill= Color.WHITE } })
//                    chaptersListView.items.addAll(it.chapters)
                        ReadAPI.chapterList.addAll(it.chapters)
                    }
                }
            }
        }
    }
    inner class ReadResponsePane(private val readResponse: ReadResponse) :ResponsePane(){
        override val handleMouseEvent: EventHandler<MouseEvent>
            get()= EventHandler{loadDetails(readResponse,image)}
        init {
            image = Image(readResponse.posterUrl,
                MainController.imageWidth,
                MainController.imageHeight, false, false, true)
            imageView.image = image
            Platform.runLater{
                if(readResponse.episodeTxt != null && readResponse.episodeTxt?.isNotEmpty() == true) {
                    episodesLabel.text=readResponse.episodeTxt
                    children.add(episodesLabel)
                }
            }
            this.onMouseClicked=handleMouseEvent
        }
    }
    fun onAddToLibrary(){
        println("added")
    }
    private fun ReadResponse.forFlowPane()=ReadResponsePane(this)


    inner class ChapterListCell: ListCell<SChapter>() {
        private val DEFAULT_COLOR="-fx-background-color: black; -fx-text-fill: white"
        private val SELECTED_COLOR="-fx-background-color: blueviolet; -fx-text-fill: white"
        private var lastItem: SChapter?=null
        private val selectedProperty = SimpleBooleanProperty(false)

        init {
            selectedProperty.addListener{ _, _, newValue ->
                style = if(newValue) SELECTED_COLOR else DEFAULT_COLOR
            }
            setOnMouseClicked {
                if(item!=null){
                    if(it.clickCount==2){
                        loadChapter(this.index)
                    }else{
                        item.isSelected.value = !item.isSelected.value
                    }
                }
            }


        }
        override fun updateItem(item: SChapter?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null
                graphic = null
                style = DEFAULT_COLOR
                if (lastItem != null){
                    selectedProperty.unbind()
                }
            } else {
                selectedProperty.bind(item.isSelected)
                text = item.chapterName
                lastItem = item
            }
        }
    }
    inner class ReadItemCell: GridCell<ResponsePane>(){
        init {
            alignment= Pos.CENTER
            background= Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
        }
        override fun updateItem(item: ResponsePane?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null;
                graphic = null;
            } else {
                onMouseClicked=item.handleMouseEvent
                graphic = item
            }
        }
    }
}

