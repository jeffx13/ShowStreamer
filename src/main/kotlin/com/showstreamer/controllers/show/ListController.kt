package com.showstreamer.controllers.show

import com.showstreamer.controllers.MainController
import com.showstreamer.parsers.ListType
import com.showstreamer.parsers.watch.ShowResponse
import com.showstreamer.parsers.WatchReadListManger
import javafx.event.EventHandler
import javafx.fxml.Initializable
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import java.net.URL
import java.util.*

class ListController:Initializable {

    lateinit var completedListView: ListView<ResponsePane>
    lateinit var completedVbox: VBox
    lateinit var droppedListView: ListView<ResponsePane>
    lateinit var droppedVbox: VBox
    lateinit var onHoldListView: ListView<ResponsePane>
    lateinit var onHoldVbox: VBox
    lateinit var currentlyWatchingListView: ListView<ResponsePane>
    lateinit var currentlyWatchingVbox: VBox

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        currentlyWatchingListView.setCellFactory { listCell() }
        completedListView.setCellFactory { listCell() }
        droppedListView.setCellFactory { listCell() }
        onHoldListView.setCellFactory { listCell() }

        WatchReadListManger.watchlist.forEach {
            addShow(it)
        }

    }

    fun addShow(it: ShowResponse) {
        val responsePane=ShowResponsePane(it)
        when(it.listType){
            ListType.CURRENT->currentlyWatchingListView.items.add(responsePane)
            ListType.ONHOLD->onHoldListView.items.add(responsePane)
            ListType.DROPPED->droppedListView.items.add(responsePane)
            ListType.COMPLETED->completedListView.items.add(responsePane)
            ListType.NONE->{currentlyWatchingListView.items.add(responsePane.also { it.bindWidth(currentlyWatchingListView) })}
        }
    }

    class listCell:ListCell<ResponsePane>(){
        init {
            style="-fx-background-color:black;"
        }
        override fun updateSelected(selected: Boolean) {
//            super.updateSelected(selected)
        }

        override fun updateItem(item: ResponsePane?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null;
                graphic = null;
            } else {
                onMouseClicked=item.handleMouseEvent
                item.imageView.fitWidthProperty().bind(listView.widthProperty().multiply(0.8))
                item.imageView.fitHeightProperty().bind(item.imageView.fitWidthProperty().multiply(MainController.aspectRatio))
                graphic = item
            }
        }
    }
    inner class ShowResponsePane(private val showResponse: ShowResponse):ResponsePane(){
        override val handleMouseEvent: EventHandler<MouseEvent>
            get() {
                return EventHandler<MouseEvent> {
                    MainController.watchSearchController.loadDetails(showResponse,image)
//                    ShowStreamer.showInfoController.setInfo(ShowHomeController.currentSelection.value, image)
                }
            }
        init {
            image = Image(showResponse.posterUrl,
                currentlyWatchingListView.width,
                currentlyWatchingListView.width*MainController.aspectRatio, false, true, true)
            imageView.image = image
        }
        fun bindWidth(listView: ListView<ResponsePane>){

        }
    }
}