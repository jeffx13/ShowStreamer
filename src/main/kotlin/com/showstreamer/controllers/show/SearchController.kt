package com.showstreamer.controllers.show

import com.showstreamer.*
import com.showstreamer.controllers.MainController
import com.showstreamer.parsers.*
import com.showstreamer.parsers.ProviderListCell
import com.showstreamer.parsers.SearchType
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.watch.TvAPI
import com.showstreamer.parsers.watch.providers.animeproviders.AnimePahe
import com.showstreamer.parsers.watch.providers.animeproviders.Gogoanime
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime
import com.showstreamer.parsers.watch.providers.mixedproviders.Iyf
import com.showstreamer.parsers.watch.providers.mixedproviders.Mudvod
import com.showstreamer.parsers.watch.providers.tvmovieproviders.Bflix
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.effect.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Callback
import kotlinx.coroutines.*
import org.controlsfx.control.GridCell
import org.controlsfx.control.GridView
import org.controlsfx.control.SearchableComboBox
import java.net.URL
import java.util.*

class SearchController : Initializable {
    companion object {
        val providers= arrayListOf(
//            NineAnime(),
//            Bflix(),
                Iyf(),
            Gogoanime(),
            Mudvod(),
            //Tangrenjie(),
            AnimePahe(),
        )
        private lateinit var providerMap:HashMap<String, ShowProvider>
        fun getProviderByName(providerName:String): ShowProvider {
            if(!this::providerMap.isInitialized){
                providerMap= HashMap()
                providers.forEach{
                    providerMap[it.name] = it
                }
            }
            return providerMap[providerName]!!
        }
        var currentSelection= SimpleObjectProperty<ShowResponse>()
        val showHomeScope = CoroutineScope(CoroutineName("Show Home Scope"))
    }
    //region FXML
    lateinit var showGridView: GridView<ResponsePane>
    lateinit var latestButton: Button
    lateinit var popularButton: Button
    lateinit var loadMoreButton: Button
    lateinit var searchButton: Button
    lateinit var titleTextField: TextField
    lateinit var providerComboBox: SearchableComboBox<ShowProvider>
    lateinit var typeComboBox: ComboBox<TvType>
    //endregion

    val showResponses=FXCollections.observableList(mutableListOf<ResponsePane>())
    private var loadDetailsJob:Job? = null
    var pressedLatest=false
    var cache = mutableListOf<ShowResponse>()
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        TvAPI.init()
        initComboBoxes()
        initListeners()
        initGridView()
        loadMoreButton.setOnAction {
            TvAPI.loadMore(null){
                addResponses(it)
            }
        }
        Platform.runLater {
            ShowStreamer.mainController.searchButton.fire()
            latestButton.fire()
        }
    }
    private fun initComboBoxes(){
        providerComboBox.items.addAll(providers)
        providerComboBox.selectionModel.select(0)

        providerComboBox.cellFactory=Callback<ListView<ShowProvider>, ListCell<ShowProvider>>{ ProviderListCell() }
        providerComboBox.buttonCell= ProviderListCell()
        providerComboBox.selectionModel.select(0)

        typeComboBox.items.addAll(TvType.values().sorted())
        typeComboBox.value = TvType.Anime
    }
    private fun initGridView(){
        showGridView.setCellFactory { ShowItemCell() }
        showGridView.items=showResponses
        showGridView.horizontalCellSpacing=5.0
        Platform.runLater {
            val bar=showGridView.lookup(".scroll-bar") as ScrollBar
            bar.valueProperty().addListener { _, _, _ ->
                if(bar.value == bar.max && !loadMoreButton.isDisable) {
                    loadMoreButton.fire()
                }
            }
            bar.visibleProperty().addListener { observable, _, _ ->
                if (!observable.value && !loadMoreButton.isDisable) {
                    showHomeScope.launch {
                        TvAPI.loadMore(null) {
                            addResponses(it)
                            if (it.isNotEmpty()) {
                                TvAPI.loadMore(null) {
                                    addResponses(it)
                                }
                            }
                        }
                    }
                }
            }
            val perRow=SimpleIntegerProperty(5)
            showGridView.cellWidthProperty().bind((showGridView.widthProperty().subtract(perRow.multiply(5))).divide(perRow.multiply(1.053)))
            showGridView.cellHeightProperty().bind(showGridView.cellWidthProperty().multiply(MainController.aspectRatio))
            ShowStreamer.primaryStage.maximizedProperty().addListener { _, _, newValue ->
                if(newValue)perRow.value=8 else perRow.value=5
            }
        }
    }
    private fun initListeners(){
        providerComboBox.valueProperty().addListener { _, oldValue, newValue ->
            if(newValue==null) {providerComboBox.value=oldValue; return@addListener}
            TvAPI.showSearchHistory=null
            //ShowStreamer.showInfoController.clearInfo()
        }
        initGridView()
        titleTextField.setOnKeyPressed { if (it.code == KeyCode.ENTER) searchButton.fire() }
        val onChangeProviderOrType = ChangeListener<Any>(){ _, oldValue, newValue ->
            if(newValue==null)return@ChangeListener

            if(newValue!=oldValue && oldValue!=null) {
                Platform.runLater{
                    showResponses.clear()
                    if(TvAPI.showSearchHistory?.type!= SearchType.LATEST){latestButton.fire()}
                }
            }
        }
        typeComboBox.valueProperty().addListener(onChangeProviderOrType)
        providerComboBox.valueProperty().addListener(onChangeProviderOrType)


    }
    private fun setResponses(results: List<ShowResponse>){
        if(results.isEmpty()){loadMoreButton.isDisable=true;return }
        Platform.runLater {
            showResponses.clear()
            addResponses(results)
            loadMoreButton.isDisable=false
        }
    }
    private fun addResponses(results: List<ShowResponse>){
        Platform.runLater{
            if(results.isEmpty()){loadMoreButton.isDisable=true;return@runLater }
            results.forEach { showResponses.add(it.forGridView())  }
            loadMoreButton.isDisable=false
        }
    }
    fun onLoadResults(event: ActionEvent){
        val searchType=when(event.source){
            searchButton -> SearchType.SEARCH
            latestButton -> SearchType.LATEST
            popularButton -> SearchType.POPULAR
            else -> return
        }
        TvAPI.loadResults(searchType){
            setResponses(it)
            MainController.homeController.bannerListView.items.addAll(it)
//            it.forEach { resp-> MainController.homeController.bannerListView.items.add(MainController.) }

        }
    }
    fun onLoadWatchList(){
        showResponses.clear()
        WatchReadListManger.watchlist.forEach {
            showResponses.add(it.forGridView())
        }
        TvAPI.updateHistory(SearchType.WATCHLIST)
    }
    fun loadDetails(showResponse: ShowResponse, image:Image){
        showHomeScope.launch {
            loadDetailsJob?.cancelAndJoin()
            loadDetailsJob = launch {
                val provider = getProviderByName(showResponse.provider)
                currentSelection.value = cache.find { it.url ==showResponse.url } ?: provider.loadDetails(showResponse).also { cache.add(it) }
                ShowStreamer.mainController.infoBtn.fire()
                MainController.watchInfoController.setInfo(currentSelection.value,image)
            }
        }

    }
    inner class ShowResponsePane(private val showResponse: ShowResponse): ResponsePane() {
        override val handleMouseEvent:EventHandler<MouseEvent>
            get() {
                return EventHandler<MouseEvent>{
                    loadDetails(showResponse,image)
                }
            }
        init {
            Platform.runLater{
                image = Image(showResponse.posterUrl,
                    showGridView.cellWidth,
                    showGridView.cellHeight, false, true, true)
                imageView.image = image
                imageView.fitWidthProperty().bind(showGridView.cellWidthProperty())
                imageView.fitHeightProperty().bind(showGridView.cellHeightProperty())
                titleLabel.text=showResponse.title
                if(showResponse.episodeTxt != null && showResponse.episodeTxt?.isNotEmpty() == true) {
                    episodesLabel.text=showResponse.episodeTxt
                    children.add(episodesLabel)
                }
            }
        }
    }
    inner class ShowItemCell:GridCell<ResponsePane>(){
        init {
            alignment=Pos.CENTER
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
    private fun ShowResponse.forGridView()=ShowResponsePane(this)
}
abstract class ResponsePane: StackPane() {
    protected lateinit var image: Image
    val imageView= ImageView()
    val titleLabel= Label().apply {
        setAlignment(this, Pos.BOTTOM_CENTER)
        isWrapText=true
        alignment= Pos.CENTER
        background = Background(BackgroundFill(Color(0.5411764979362488,0.16862745583057404,0.886274516582489,0.7), CornerRadii(5.0), Insets.EMPTY))
        font= Font.font("Comic Sans MS", FontWeight.SEMI_BOLD, FontPosture.ITALIC, 14.0)
        style="-fx-text-fill: white;-fx-effect: dropshadow(three-pass-box, derive(cadetblue, -20%), 10, 0, 4, 4); "
        effect=DropShadow(BlurType.TWO_PASS_BOX, Color.YELLOW,10.0,10.0,10.0,10.0)
    }
    val episodesLabel = Label().apply {
        setAlignment(this, Pos.TOP_RIGHT)
        alignment = Pos.CENTER
        minWidth = 50.0
        prefWidth = 100.0
        textFill = Color.BLACK
        background= Background(BackgroundFill(Color.RED, CornerRadii(10.0), Insets.EMPTY))
        font= Font.font("Comic Sans MS", FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 12.0)
        effect=DropShadow(BlurType.THREE_PASS_BOX, Color.YELLOW,10.0,0.0,4.0,4.0)
    }
    abstract val handleMouseEvent: EventHandler<MouseEvent>
    init {

        this.children.addAll(imageView,titleLabel)
        this.hoverProperty().addListener { _, _, newValue ->
            if(newValue){
                val adj=ColorAdjust(0.0, 0.1, 0.0, 0.0).apply { input=Glow().apply { this.level=0.6 } }
                effect=adj
            }else{
                effect=Glow().apply { this.level=-0.5 }
            }
        }
    }
}
