package com.showstreamer.controllers

import com.showstreamer.controllers.reader.ReadController
import com.showstreamer.controllers.reader.ReaderHomeController
import com.showstreamer.controllers.show.*
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import org.kordamp.ikonli.javafx.FontIcon
import java.awt.Desktop
import java.awt.Toolkit
import java.io.File
import java.net.URL
import java.util.*


class MainController : Initializable {
    lateinit var homeButton: Button
    lateinit var mainBorderPane: BorderPane
    lateinit var leftVbox: VBox
    lateinit var rightVbox: VBox
    var currentPaneButton=SimpleObjectProperty<Button>()
    lateinit var backwardsButton: Button
    lateinit var forwardButton: Button
    lateinit var playPauseButton: ToggleButton
    lateinit var currentlyPlayingLabel: Label
    lateinit var contentAnchorPane: AnchorPane
    lateinit var watchListPane: AnchorPane
    lateinit var watchListPaneController: ListController
    lateinit var readerHomePaneController: ReaderHomeController
    lateinit var readBtn: Button
    lateinit var readerHomePane:AnchorPane
    lateinit var readerHomeBtn: Button
    lateinit var mangaReadListBtn: Button
    lateinit var readPane:BorderPane
    lateinit var readPaneController:ReadController


    lateinit var loadPlaylistBtn: Button
    lateinit var watchListBtn: Button
    lateinit var topAnchorPane: BorderPane
    lateinit var closeAppButton: Button
    lateinit var resizeButton: Button
    lateinit var minimizeButton: Button

    lateinit var settingsBtn: Button
    lateinit var settingsPane: AnchorPane
    lateinit var settingsPaneController: SettingsController

    lateinit var downloadPane: AnchorPane
    lateinit var infoPane: AnchorPane
    lateinit var homePane: AnchorPane
    lateinit var videoPane:BorderPane
    @FXML
    lateinit var infoPaneController: ShowInfoController
    @FXML
    lateinit var videoPaneController: MpvController
    @FXML
    lateinit var downloadPaneController: DownloadController

    lateinit var searchButton: Button
    lateinit var infoBtn: Button
    lateinit var downloadBtn: Button
    lateinit var mpvBtn: Button
    lateinit var currentPane:Pane
    lateinit var scenes:List<Parent>

    companion object{
        lateinit var watchSearchController: SearchController
        lateinit var watchInfoController: ShowInfoController

        lateinit var watchSearchScene:Parent
        lateinit var watchInfoScene:Parent

        lateinit var mpvScene:Parent
        lateinit var mpvController: MpvController

        lateinit var settingsScene:Parent
        lateinit var settingsController: SettingsController

        lateinit var readHomeScene:Parent
        lateinit var readHomeController: ReaderHomeController

        lateinit var readViewScene:Parent
        lateinit var readViewController: ReadController

        lateinit var downloadScene:Parent
        lateinit var downloadController: DownloadController

        lateinit var listScene:Parent
        lateinit var listController:ListController

        lateinit var homeScene:Parent
        lateinit var homeController:HomeController

        val currentScene = SimpleObjectProperty<Parent>()

        lateinit var watchModeButtons:List<Button>
        lateinit var readModeButtons:List<Button>
        const val aspectRatio=1.42857142857
        var downloadWorkDir:String? = "D:\\TV\\temp"
        //private val urlRegex = """((http|ftp|https)://([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:/~+#-]*[\w@?^=%&/~+#-])?)""".toRegex()
        //val refererRegex = "[rR]eferer:(.*?)\"".toRegex()
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        const val nilaodaPath = "C:\\Users\\Jeffx\\Downloads\\N_m3u8DL-CLI_v3.0.2_with_ffmpeg_and_SimpleG\\N_m3u8DL-CLI_v3.0.2.exe"
        var imageWidth = 250.00
        var imageHeight = 380.00
        fun openDirectory(directory: String) {
            Desktop.getDesktop().open( File(directory))
        }
        fun browseDirectory(directory: String): File? {
            val directoryChooser = DirectoryChooser()
            directoryChooser.initialDirectory = File(directory)
            return directoryChooser.showDialog(null)
        }

    }



    override fun initialize(location: URL?, resources: ResourceBundle?) {
        Platform.runLater {
            initScenes()
            bindButtonProperties()
            setButtonActions()
            scenes=listOf(watchSearchScene, watchInfoScene)
            currentPaneButton.value = searchButton
            watchModeButtons=listOf(watchListBtn,searchButton,infoBtn,mpvBtn,downloadBtn,settingsBtn)
            readModeButtons= listOf(readerHomeBtn,readBtn)
        }
        currentPaneButton.addListener { _, oldValue, _ ->
            if(oldValue!=null){
                (oldValue.graphic as FontIcon).iconColor=Color.WHITE
            }
        }

        Platform.runLater {  }

        //TODO() Anime page gridview episodes
    }
    private fun initScenes(){
        val watchSearchSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/search.fxml"))
        watchSearchScene= watchSearchSceneLoader.load()
        watchSearchController=watchSearchSceneLoader.getController()
        this.mainBorderPane.center=watchSearchScene

        val watchInfoSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/info.fxml"))
        watchInfoScene = watchInfoSceneLoader.load()
        watchInfoController=watchInfoSceneLoader.getController()

        val mpvSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/mpv.fxml"))
        mpvScene = mpvSceneLoader.load()
        mpvController=mpvSceneLoader.getController()

        val settingsSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/settings.fxml"))
        settingsScene = settingsSceneLoader.load()
        settingsController=settingsSceneLoader.getController()

        val readHomeSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/read/home.fxml"))
        readHomeScene = readHomeSceneLoader.load()
        readHomeController=readHomeSceneLoader.getController()

        val readViewSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/read/view.fxml"))
        readViewScene = readViewSceneLoader.load()
        readViewController=readViewSceneLoader.getController()

        val downloadSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/download.fxml"))
        downloadScene = downloadSceneLoader.load()
        downloadController=downloadSceneLoader.getController()

        val listSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/watch/list.fxml"))
        listScene = listSceneLoader.load()
        listController=listSceneLoader.getController()

        val homeSceneLoader = FXMLLoader(this.javaClass.getResource("/FXML/home.fxml"))
        homeScene = homeSceneLoader.load()
        homeController=homeSceneLoader.getController()

        currentScene.value= watchSearchScene
    }


    private fun setButtonActions(){
        backwardsButton.setOnAction {
            mpvController.changePlaylistPos(-1)
        }
        playPauseButton.selectedProperty().addListener { _, _, newValue ->
            (playPauseButton.graphic as FontIcon).iconLiteral=if(newValue) "fas-pause" else "fas-play"
        }
        forwardButton.setOnAction {
            mpvController.changePlaylistPos(1)
        }
        loadPlaylistBtn.setOnAction {
            mpvController.loadPlaylistFromFolder()
        }
        playPauseButton.setOnAction {
            mpvController.playPause()
            playPauseButton.isSelected=!mpvController.isPausedProperty.value
        }
    }
    private fun bindButtonProperties(){
        playPauseButton.disableProperty().bind(mpvController.currentVideo.isNull)
        playPauseButton.selectedProperty().bindBidirectional(mpvController.isPausedProperty)
        forwardButton.isDisable=true
        mpvController.currentPlaylist.addListener { observable, _, newValue ->
            forwardButton.isDisable = if(newValue.size==0)true else mpvController.currentIndex.value==observable.value.size-1
        }
        backwardsButton.disableProperty().bind(mpvController.currentIndex.greaterThan(0).not())

    }
    fun onChangePane(event: ActionEvent){
        if(event.source==currentPaneButton.value)return
        val scene = when(event.source){
            watchListBtn -> listScene
            searchButton -> watchSearchScene
            homeButton -> homeScene
            infoBtn -> watchInfoScene
            mpvBtn -> mpvScene
            downloadBtn -> downloadScene
            settingsBtn -> settingsScene
            readerHomeBtn -> readHomeScene
            readBtn -> readViewScene
            else ->  return
        }
       Platform.runLater {
           if(scene== mpvScene)mpvController.show() else mpvController.hide()
           mainBorderPane.center=scene
           currentScene.value=scene

           val fontIcon=(event.source as Button).graphic as FontIcon
           fontIcon.iconColor=Color.BLUEVIOLET
           currentPaneButton.value=event.source as Button
       }
    }

    fun prevTab(){
        var nextPaneIndex= buttonList.indexOf(currentPaneButton.value)-1
        if(nextPaneIndex<0)nextPaneIndex= buttonList.size-1
        buttonList[nextPaneIndex].fire()
    }
    private val buttonList
        get() = if(currentPaneButton.value in watchModeButtons)watchModeButtons else readModeButtons
    fun nextTab(){
        var nextPaneIndex= buttonList.indexOf(currentPaneButton.value)+1
        if(nextPaneIndex>= buttonList.size)nextPaneIndex=0
        buttonList[nextPaneIndex].fire()
    }
}
