package com.showstreamer.controllers.reader

import com.showstreamer.ShowStreamer
import com.showstreamer.controllers.MainController
import javafx.animation.*
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.Duration
import org.controlsfx.control.GridView
import org.controlsfx.control.cell.ImageGridCell
import java.net.URL
import java.util.*


class ReadController:BorderPane(),Initializable {
    private var autoScroll = SimpleBooleanProperty(false)
    private var autoScrollSpeed:Double=50.0

    lateinit var readScrollBar: ScrollBar
    lateinit var readListView: ListView<Pair<Int,Image>>
    private var scaleValue = 0.7
    private val zoomIntensity = 0.02
    private var currentZoom=0.0
    private lateinit var zoomNode: Node
    private lateinit var target:Node
//    private val animation: Animation
    companion object{
        var currentChapter=1
    }
    private fun changeChapter(chapter:Int,absolute:Boolean=false){
        if(absolute){

        }else{
            MainController.readHomeController.loadChapter(currentChapter+chapter)
        }
        val gridView= GridView<Image>()
        gridView.cellFactory=Callback { ImageGridCell() }

    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        background= Background(BackgroundFill(Color.RED, CornerRadii(0.0), Insets(0.0)))
        readListView.setCellFactory { ReadListCell() }
        Platform.runLater {
//            readScrollBar = readListView.lookup(".scroll-bar") as ScrollBar
//            val animation = Timeline(KeyFrame(Duration.seconds(50.0), KeyValue(readScrollBar.valueProperty(), 1)))
//            autoScroll.addListener { _, _, newValue ->
//                if(newValue)animation.play() else animation.pause()
//            }
        }

        readListView.setOnKeyPressed { handleKeyEvents(it) }
        this.setOnKeyPressed { readListView.requestFocus(); handleKeyEvents(it) }
    }
    private fun handleKeyEvents(e: KeyEvent){
        e.consume()
        when(e.code){
            KeyCode.SPACE->Platform.runLater{ autoScroll.value=!autoScroll.value}
            KeyCode.RIGHT->{changeChapter(1)}
            KeyCode.LEFT->{changeChapter(-1)}
            KeyCode.D->{}
            KeyCode.ENTER->{refreshListView()}
            KeyCode.TAB->readListView.scrollTo(0)
            KeyCode.PAGE_DOWN->{
                readListView.scrollTo(readListView.items.size-1)}
            KeyCode.PAGE_UP->{
                readListView.scrollTo(0)}
            else->{}
        }
        readListView.requestFocus()
    }
    private fun refreshListView(){
        for(index in 0 until readListView.items.size){
            readListView.scrollTo(readListView.items.size-1)
        }

    }
    val outerNodes= mutableListOf<ZoomableScrollPane>()
    inner class ReadListCell: ListCell<Pair<Int,Image>>() {
        private val imageView = ImageView()
        private val scrollPane=ZoomableScrollPane(imageView)
        private val pause = PauseTransition(Duration.millis(1000.0))
        init {
            background= Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
            imageView.fitWidthProperty().bind(scrollPane.widthProperty())
            imageView.isPreserveRatio=true
        }
        override fun updateItem(item: Pair<Int,Image>?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                Platform.runLater {
                    text = null
                    graphic = null
                }
            } else {
                Platform.runLater {
                    imageView.image = item.second;
                    pause.onFinished = EventHandler { e: ActionEvent? ->
                        this.prefWidthProperty().bind(listView.widthProperty().multiply(0.9))
                        scrollPane.prefWidthProperty().bind(this.widthProperty())
                        graphic = scrollPane;
                    }
                    pause.play()
                }

            }
        }
    };
    inner class ZoomableScrollPane(target: Node) : ScrollPane() {
        private var scaleValue = 0.7
        private val zoomIntensity = 0.1
        private val target: Node
        private val zoomNode: Node
        init {
            this.target = target
            zoomNode = Group(target)
            content = outerNode(zoomNode)
            isPannable = true
            hbarPolicy = ScrollBarPolicy.NEVER
            vbarPolicy = ScrollBarPolicy.NEVER
            isFitToHeight = true //center
            isFitToWidth = true //center
            updateScale()

        }

        private fun outerNode(node: Node): Node {
            val outerNode = centeredNode(node)
            outerNodes.add(this)
            outerNode.onScroll = EventHandler { e: ScrollEvent ->
                e.consume()
                if(e.isControlDown){
                    outerNodes.forEach {
                        it.onScroll(e.textDeltaY, Point2D(e.x, e.y))
                    }
//                    onScroll(e.textDeltaY, Point2D(e.x, e.y))
                }else{
                    readScrollBar.value-=e.deltaY/(readListView.width*5)
                }

            }
            outerNode.setOnKeyPressed { handleKeyEvents(it) }
            this.setOnKeyPressed { handleKeyEvents(it) }
            return outerNode
        }

        private fun centeredNode(node: Node): Node {
            val vBox = VBox(node)
            vBox.alignment = Pos.CENTER
            return vBox
        }

        private fun updateScale() {
            target.scaleX = scaleValue
            target.scaleY = scaleValue
        }

        private fun onScroll(wheelDelta: Double, mousePoint: Point2D) {
            val zoomFactor = Math.exp(wheelDelta * zoomIntensity)
            val innerBounds = zoomNode.layoutBounds
            val viewportBounds = viewportBounds

            // calculate pixel offsets from [0, 1] range
            val valX = hvalue * (innerBounds.width - viewportBounds.width)
            val valY = vvalue * (innerBounds.height - viewportBounds.height)
            scaleValue = scaleValue * zoomFactor
            updateScale()
            layout() // refresh ScrollPane scroll positions & target bounds

            // convert target coordinates to zoomTarget coordinates
            val posInZoomTarget = target.parentToLocal(zoomNode.parentToLocal(mousePoint))

            // calculate adjustment of scroll position (pixels)
            val adjustment = target.localToParentTransform.deltaTransform(posInZoomTarget.multiply(zoomFactor - 1))

            // convert back to [0, 1] range
            // (too large/small values are automatically corrected by ScrollPane)
            val updatedInnerBounds = zoomNode.boundsInLocal
            hvalue = (valX + adjustment.x) / (updatedInnerBounds.width - viewportBounds.width)
            vvalue = (valY + adjustment.y) / (updatedInnerBounds.height - viewportBounds.height)
        }
    }
}

