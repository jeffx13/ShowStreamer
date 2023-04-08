package com.showstreamer.controllers.show

import com.showstreamer.*
import com.showstreamer.controllers.*
import com.showstreamer.controllers.show.SearchController.Companion.showHomeScope
import com.showstreamer.parsers.*
import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.watch.TvAPI
import com.showstreamer.utils.*
import javafx.animation.Interpolator
import javafx.animation.Transition
import javafx.application.Platform
import javafx.fxml.Initializable
import javafx.geometry.Rectangle2D
import javafx.scene.control.*
import javafx.scene.effect.ColorAdjust
import javafx.scene.effect.GaussianBlur
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.FlowPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.converter.IntegerStringConverter
import kotlinx.coroutines.*
import org.kordamp.ikonli.javafx.FontIcon
import java.net.URL
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.*
import java.util.function.UnaryOperator


class ShowInfoController : Initializable{

    lateinit var genresFlowPane: FlowPane
    lateinit var titleLabel: Label
    lateinit var descriptionLabel: Label
    lateinit var infoVBox: VBox
    lateinit var infoStackPane: StackPane
    lateinit var backgroundImageView: ImageView
    private var loadLinksJob: Job? = null
    //region FXML
    lateinit var listTypeComboBox: ComboBox<ListType>
    lateinit var openDirectoryButton: Button
    lateinit var browseDirectoryButton: Button
    lateinit var deselectAllButton: Button
    lateinit var selectAllButton: Button
    lateinit var selectButton: Button
    lateinit var episodesListView: ListView<SEpisode>
    lateinit var addToWatchListButton: Button
    lateinit var mainPlayButton: Button
    lateinit var mainDownloadButton: Button
    lateinit var directoryTextField: TextField
    lateinit var episodeEndSpinner: Spinner<Int>
    lateinit var episodeStartSpinner: Spinner<Int>
    lateinit var linkProgressBar: ProgressBar
    lateinit var showInfoListView: ListView<String>
    lateinit var infoImageView: ImageView
    private val episodeEndSpinnerFactory= SpinnerValueFactory.IntegerSpinnerValueFactory(1, 2000, 1)
    val playlistAddedIcon = FontIcon("mdmz-playlist_add_check").apply {
        iconColor= Color.GREEN
        iconSize=20
    }
    val playlistAddIcon = FontIcon("mdmz-playlist_add").apply {
        iconColor= Color.RED
        iconSize=20
    }
    //endregion
    private fun clearInfo(){
        infoImageView.image=null
        showInfoListView.items.clear()
        episodesListView.items.clear()
    }
    private fun setButtonActions(){
        selectButton.setOnAction {
            val totalEpisodes=episodesListView.items.size
            if(episodeStartSpinner.value==episodeEndSpinner.value){
                episodesListView.items[totalEpisodes-episodeEndSpinner.value].isSelected.value=true
            }
            else {
                val start = totalEpisodes-episodeEndSpinner.value
                val end = totalEpisodes-episodeStartSpinner.value+1
                episodesListView.items.subList(start,end).forEach { it.isSelected.value = true }
            }
        }
        selectAllButton.setOnAction { episodesListView.items.forEach{ it.isSelected.value=true } }
        deselectAllButton.setOnAction { episodesListView.items.forEach{ it.isSelected.value=false } }
        browseDirectoryButton.setOnAction {
            browseDirectoryButton.isDisable=true
            val newPath = MainController.browseDirectory(directoryTextField.text)
            if (newPath!=null)directoryTextField.text = newPath.absolutePath
            browseDirectoryButton.isDisable=false
        }
        openDirectoryButton.setOnAction { MainController.openDirectory(directoryTextField.text) }
    }
    fun onDownload() {
        showHomeScope.launch {
            loadLinksJob?.join()
            loadLinksJob=launch {
                loadLinks { videos -> if(videos.isNotEmpty())MainController.downloadController.addToDownloadQueue(videos)}
            }
        }
    }
    fun onAddToWatchList() {
        if(SearchController.currentSelection.value==null)return
        val show = SearchController.currentSelection.value!!
        if(addToWatchListButton.text=="Add to Playlist"){
            WatchReadListManger.addToWatch(show)
            addToWatchListButton.graphic=playlistAddedIcon
            addToWatchListButton.text="Added to Playlist"
        }else{
            WatchReadListManger.removeFromWatch(show)
            addToWatchListButton.graphic=playlistAddIcon
            addToWatchListButton.text="Add to Playlist"
        }

    }
    fun onPlay() {
        if (SearchController.currentSelection.value != null) {
            mainPlayButton.isDisable = true
            showHomeScope.launch {
                loadLinksJob?.cancelAndJoin()
                loadLinksJob= launch {
                    loadLinks { videos ->
                        if(videos.isNotEmpty()){
                            videos.sortBy {
                                if(it.name.startsWith("Episode")){
                                    it.name.substringBetween("Episode ",":").toIntOrNull()
                                }else{it.name.toIntOrNull()}
                            }
                            MainController.mpvController.loadPlaylist(videos)
                            ShowStreamer.mainController.mpvBtn.fire()
                        }
                        mainPlayButton.isDisable = false

                    }

                }
            }
        }
    }
    private suspend fun loadLinks(callbackAll: (MutableList<Video>)->Unit) {
        linkProgressBar.progress=0.0
        val episodes = episodesListView.items.filter { it.isSelected.value }
        deselectAllButton.fire()
        if(episodes.isEmpty()) return
        val provider = SearchController.getProviderByName(SearchController.currentSelection.value!!.provider)
        TvAPI.loadSources(episodes,provider){
            callbackAll(it)
        }
    }
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        directoryTextField.text = MainController.downloadWorkDir
        episodesListView.cellFactory = Callback<ListView<SEpisode>, ListCell<SEpisode>>{ EpisodeListCell() }
        showInfoListView.cellFactory=Callback<ListView<String>, ListCell<String>>{ InfoListCell() }

        listTypeComboBox.cellFactory= Callback<ListView<ListType>, ListCell<ListType>> {ListTypeCell() }
        listTypeComboBox.buttonCell=ListTypeCell()

        listTypeComboBox.items.addAll(ListType.values())
        initSpinners()
        initBackgroundTransition()
        setButtonActions()

    }
    class ListTypeCell: ListCell<ListType>() {
        override fun updateItem(item: ListType?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                text=item.type
            }
        }
    }
    private fun initSpinners(){
        val format = NumberFormat.getIntegerInstance()
        val filter = UnaryOperator { c: TextFormatter.Change ->
            if (c.isContentChange) {
                c.text=c.text.filter { it.isDigit() }
                val parsePosition = ParsePosition(0)
                format.parse(c.controlNewText, parsePosition)
                if (parsePosition.index == 0 ||
                    parsePosition.index < c.controlNewText.length
                ) {
                    c.text=c.text.filter { it.isDigit() }
                }
            }
            c
        }
        val episodeStartSpinnerFactory= SpinnerValueFactory.IntegerSpinnerValueFactory(1, 2000, 1)
        episodeStartSpinner.valueFactory = episodeStartSpinnerFactory
        episodeEndSpinner.valueFactory = episodeEndSpinnerFactory
        episodeStartSpinner.editor.textFormatter=TextFormatter(IntegerStringConverter(), 1,filter)
        episodeEndSpinner.editor.textFormatter=TextFormatter(IntegerStringConverter(),1,filter)
        SearchController.currentSelection.addListener { _, _, newValue ->
            if(newValue!=null){
                episodeEndSpinnerFactory.max=newValue.totalEpisodes
            }
        }
        episodeEndSpinner.valueProperty().addListener{_, _, newValue ->
            if(newValue==null){
                episodeEndSpinner.valueFactory.value=1
            }
        }
        episodeStartSpinner.valueProperty().addListener{ _, _, newValue ->
            if(newValue==null) {
                episodeStartSpinner.valueFactory.value = 1
            } else if(newValue>episodeEndSpinner.value){
                episodeStartSpinner.valueFactory.value=episodeEndSpinner.value
            }
        }

    }
    fun initBackgroundTransition(){
        backgroundImageView.fitWidthProperty().bind(infoStackPane.widthProperty())
        backgroundImageView.fitHeightProperty().bind(backgroundImageView.fitWidthProperty().multiply(MainController.aspectRatio))
        backgroundImageView.isSmooth=true
        backgroundImageView.isManaged=false
        backgroundImageView.effect = ColorAdjust(0.0, 0.0, -0.6, 0.0).apply { input= GaussianBlur(20.0) }
        genresFlowPane.hgap=10.0
        val timer = object:Transition() {
            var imageHeight = 1.0
            var imageWidth = 1.0
            var height = 1.0
            var heightFraction = 1.0
            val duration = Duration(30000.0)
            init {
                cycleDuration = duration
                interpolator = Interpolator.LINEAR
                isAutoReverse = true
                cycleCount = INDEFINITE
            }

            fun changeRatio(image: Image) {
                imageHeight = image.height
                imageWidth = image.width
                heightFraction = infoStackPane.prefHeight/infoStackPane.prefWidth
                height = heightFraction*imageHeight


            }

            override fun interpolate(frac: Double) {
                if (frac > 1 - heightFraction) {
                    playFrom(duration.multiply(1 + heightFraction));return
                }
                backgroundImageView.viewport = Rectangle2D(0.0, imageHeight * frac, imageWidth, height)
            }
        }
        backgroundImageView.imageProperty().addListener { _, _, newValue ->
            timer.stop()
            timer.changeRatio(newValue)
            timer.playFromStart()
        }
        MainController.currentScene.addListener { observable, oldValue, newValue ->
            if(newValue==MainController.watchInfoScene){
                timer.play()
            }else{
                timer.pause()
            }
        }
    }
    fun setInfo(currentSelection: ShowResponse, image:Image) {
        Platform.runLater {
            val episodes = currentSelection.episodes
            episodeEndSpinner.valueFactory.value = episodes.size
            episodeStartSpinner.valueFactory.value = episodes.size
            clearInfo()
            infoImageView.image=image
            backgroundImageView.image = image

            backgroundImageView.viewport=Rectangle2D(0.0,image.width*0.35,image.width,image.width*infoStackPane.boundsInParent.height/infoStackPane.boundsInParent.width)
//            println(image.width*infoStackPane.boundsInParent.height/infoStackPane.boundsInParent.width)
            titleLabel.text=currentSelection.title
            descriptionLabel.text=currentSelection.description
            genresFlowPane.children.addAll(currentSelection.genres.map { Label(it).apply { textFill=Color.WHITE ;style="-fx-background-radius: 10 10 10 10; -fx-background-color: rgba(255,255,255,0.3)" } })
            showInfoListView.items.addAll(
                "Episodes||${currentSelection.totalEpisodes}",
                "Year||${currentSelection.year}",
                "Ratings||${currentSelection.ratings}",
                "Views||${currentSelection.views}",
                "Update||${currentSelection.updateSchedule}",
                "Status||${currentSelection.status}",
            )

            episodes.sortByDescending { it.number }
            episodes.forEach { it.isSelected.value=false }
            Platform.runLater {
                episodes.first().isSelected.value=true
                episodesListView.items.addAll(episodes)
                mainPlayButton.isDisable=false
                mainDownloadButton.isDisable=false
            }

            if(WatchReadListManger.isInWatch(currentSelection.url)){
                addToWatchListButton.graphic = playlistAddedIcon
                addToWatchListButton.text="Added to Playlist"
            }else{
                addToWatchListButton.graphic=playlistAddIcon
                addToWatchListButton.text="Add to Playlist"
            }
        }
    }
}