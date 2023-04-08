package com.showstreamer

import com.showstreamer.controllers.*
import com.showstreamer.controllers.show.*
import com.showstreamer.parsers.watch.TvAPI
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.cancel
import java.awt.Dimension
import java.awt.Toolkit
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class ShowStreamer : Application() {
    companion object {
        const val STAGE_TITLE = "AnimeScraper!"
        lateinit var primaryStage:Stage
        lateinit var primaryScene:Scene
        lateinit var mainController:MainController
        private val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize


        fun toggleFullscreen(){
            primaryStage.isMaximized=!primaryStage.isMaximized
            if(!MainController.mpvController.mpvStage.isIconified){
                MainController.mpvController.rebasePlayer()
            }
        }
    }



    override fun start(stage: Stage) {
       try{
           val fxmlLoader = FXMLLoader(this.javaClass.getResource("/FXML/main.fxml"))
           primaryScene = Scene(fxmlLoader.load())
           primaryScene.fill=Color.TRANSPARENT
           primaryScene.stylesheets.add("css/default.css")
//        primaryScene.stylesheets.add("JMetro/dark_theme.css")
           mainController = fxmlLoader.getController()
           Platform.setImplicitExit(false)
           initStage(stage)
           setListeners()
       }catch (e:Exception){
           stop()
       }
    }
    private fun initStage(stage:Stage){
        stage.title = STAGE_TITLE
        stage.initStyle(StageStyle.TRANSPARENT)
        stage.icons.add(Image("https://9anime.id/assets/_9anime/imagesv2/bg-error3.png"))
//        stage.icons.add(Image("https://icon-library.com/images/cool-anime-icon/cool-anime-icon-9.jpg"))
        stage.scene = primaryScene
        stage.focusedProperty().addListener { observable, oldValue, newValue ->
            Platform.runLater {
//                if (mainController.currentPane == mainController.videoPane) {
//
//                }
//                MainController.mpvController.mpvStage.requestFocus()
            }
        }
        primaryStage = stage
        stage.show()
    }
    private fun setListeners(){
        mainController.closeAppButton.setOnMouseClicked {
            MainController.mpvController.mpvStage.close()
            primaryStage.close()
            Platform.exit()
        }
        mainController.resizeButton.setOnMouseClicked {
            toggleFullscreen()
        }
        mainController.topAnchorPane.setOnMouseClicked {
            if(it.clickCount==2 && it.button==MouseButton.PRIMARY ){
                it.consume()
                toggleFullscreen()

            }
        }
        mainController.topAnchorPane.setOnMousePressed {pressEvent->
            mainController.topAnchorPane.setOnMouseDragged { dragEvent ->
                if(pressEvent.button==MouseButton.PRIMARY && !primaryStage.isMaximized) {
                    if(!MainController.mpvController.mpvStage.isIconified){
                        MainController.mpvController.rebasePlayer()
                    }
                    primaryStage.x = dragEvent.screenX - pressEvent.sceneX
                    primaryStage.y = dragEvent.screenY - pressEvent.sceneY
                }
            }
        }
        mainController.minimizeButton.setOnMouseClicked {
            MainController.mpvController.mpvStage.hide()
            primaryStage.isIconified=true
        }
    }
    override fun stop() {
        super.stop()
        try {
            MainController.mpvController.stop()
            SearchController.showHomeScope.cancel()
            TvAPI.scope.cancel()
            MainController.downloadController.cancelAllButton.fire()
            MainController.downloadController.executor.shutdown()
            MainController.downloadController.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e:InterruptedException) {
            e.printStackTrace()
        }finally {
            exitProcess(0)
        }

    }
}

